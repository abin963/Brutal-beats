package com.example.data.download

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.Track
import com.example.source.YouTubeMusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadInfo(
    val videoId: String,
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val errorMessage: String? = null
) {
    val formattedSize: String
        get() = DownloadManager.formatBytes(totalBytes)
}

class DownloadManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val trackDao = database.trackDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadInfo>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadInfo>> = _downloadStates.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val downloadsDir = File(appContext.filesDir, "nyx_downloads").apply {
        if (!exists()) mkdirs()
    }

    fun isDownloaded(videoId: String): Boolean {
        val file = File(downloadsDir, "$videoId.m4a")
        return file.exists() && file.length() > 0
    }

    fun getDownloadedFile(videoId: String): File? {
        val file = File(downloadsDir, "$videoId.m4a")
        return if (file.exists() && file.length() > 0) file else null
    }

    fun getDownloadedFileSize(videoId: String): Long {
        val file = File(downloadsDir, "$videoId.m4a")
        return if (file.exists()) file.length() else 0L
    }

    fun getTotalDownloadsSizeBytes(): Long {
        return try {
            downloadsDir.listFiles()?.filter { it.isFile && it.extension == "m4a" }?.sumOf { it.length() } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun getFreeStorageBytes(): Long {
        return try {
            appContext.filesDir.usableSpace
        } catch (_: Exception) {
            0L
        }
    }

    fun downloadTrack(track: Track, onComplete: ((Boolean) -> Unit)? = null) {
        if (_downloadStates.value[track.videoId]?.status == DownloadStatus.DOWNLOADING) {
            return // Already in progress
        }

        // Check storage requirement (minimum 25 MB free)
        val freeBytes = getFreeStorageBytes()
        if (freeBytes in 1 until 25 * 1024 * 1024) {
            _downloadStates.update {
                it + (track.videoId to DownloadInfo(
                    videoId = track.videoId,
                    status = DownloadStatus.FAILED,
                    errorMessage = "Insufficient storage on device"
                ))
            }
            onComplete?.invoke(false)
            return
        }

        val job = scope.launch {
            _downloadProgress.update { it + (track.videoId to 0.01f) }
            _downloadStates.update {
                it + (track.videoId to DownloadInfo(
                    videoId = track.videoId,
                    progress = 0.01f,
                    status = DownloadStatus.DOWNLOADING
                ))
            }

            try {
                // 1. Resolve audio stream URL if not already present
                val streamUrl = track.directStreamUrl
                    ?: YouTubeMusicService.resolveStreamUrl(track.videoId)

                if (streamUrl.isNullOrBlank()) {
                    _downloadProgress.update { it - track.videoId }
                    _downloadStates.update {
                        it + (track.videoId to DownloadInfo(
                            videoId = track.videoId,
                            status = DownloadStatus.FAILED,
                            errorMessage = "Source unavailable"
                        ))
                    }
                    activeJobs.remove(track.videoId)
                    withContext(Dispatchers.Main) { onComplete?.invoke(false) }
                    return@launch
                }

                val targetFile = File(downloadsDir, "${track.videoId}.m4a")
                val tempFile = File(downloadsDir, "${track.videoId}.tmp")

                val request = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _downloadProgress.update { it - track.videoId }
                        _downloadStates.update {
                            it + (track.videoId to DownloadInfo(
                                videoId = track.videoId,
                                status = DownloadStatus.FAILED,
                                errorMessage = "HTTP error ${response.code}"
                            ))
                        }
                        activeJobs.remove(track.videoId)
                        withContext(Dispatchers.Main) { onComplete?.invoke(false) }
                        return@launch
                    }

                    val body = response.body ?: throw Exception("Empty stream body")
                    val totalBytes = body.contentLength()
                    var bytesRead = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(8 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                bytesRead += read
                                if (totalBytes > 0) {
                                    val prog = (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f)
                                    _downloadProgress.update { it + (track.videoId to prog) }
                                    _downloadStates.update {
                                        it + (track.videoId to DownloadInfo(
                                            videoId = track.videoId,
                                            progress = prog,
                                            status = DownloadStatus.DOWNLOADING,
                                            totalBytes = totalBytes,
                                            downloadedBytes = bytesRead
                                        ))
                                    }
                                }
                            }
                            output.flush()
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 0) {
                        if (targetFile.exists()) targetFile.delete()
                        tempFile.renameTo(targetFile)

                        // Update Room persistence
                        trackDao.setDownloaded(track.videoId, true, targetFile.absolutePath)
                        _downloadProgress.update { it - track.videoId }
                        _downloadStates.update {
                            it + (track.videoId to DownloadInfo(
                                videoId = track.videoId,
                                progress = 1f,
                                status = DownloadStatus.COMPLETED,
                                totalBytes = targetFile.length(),
                                downloadedBytes = targetFile.length()
                            ))
                        }
                        activeJobs.remove(track.videoId)
                        withContext(Dispatchers.Main) { onComplete?.invoke(true) }
                    } else {
                        tempFile.delete()
                        _downloadProgress.update { it - track.videoId }
                        _downloadStates.update {
                            it + (track.videoId to DownloadInfo(
                                videoId = track.videoId,
                                status = DownloadStatus.FAILED,
                                errorMessage = "Corrupted download file"
                            ))
                        }
                        activeJobs.remove(track.videoId)
                        withContext(Dispatchers.Main) { onComplete?.invoke(false) }
                    }
                }
            } catch (e: Exception) {
                _downloadProgress.update { it - track.videoId }
                _downloadStates.update {
                    it + (track.videoId to DownloadInfo(
                        videoId = track.videoId,
                        status = DownloadStatus.FAILED,
                        errorMessage = e.message ?: "Download failed"
                    ))
                }
                activeJobs.remove(track.videoId)
                withContext(Dispatchers.Main) { onComplete?.invoke(false) }
            }
        }

        activeJobs[track.videoId] = job
    }

    fun cancelDownload(videoId: String) {
        val job = activeJobs.remove(videoId)
        job?.cancel()
        _downloadProgress.update { it - videoId }
        _downloadStates.update {
            it + (videoId to DownloadInfo(
                videoId = videoId,
                status = DownloadStatus.CANCELLED
            ))
        }
        val temp = File(downloadsDir, "$videoId.tmp")
        if (temp.exists()) temp.delete()
    }

    fun deleteDownload(videoId: String) {
        cancelDownload(videoId)
        scope.launch {
            try {
                val file = File(downloadsDir, "$videoId.m4a")
                if (file.exists()) {
                    file.delete()
                }
                trackDao.setDownloaded(videoId, false, null)
                _downloadStates.update { it - videoId }
            } catch (_: Exception) {}
        }
    }

    fun clearAllDownloads() {
        scope.launch {
            try {
                downloadsDir.listFiles()?.forEach { file ->
                    if (file.isFile) file.delete()
                }
                trackDao.clearAllDownloaded()
                _downloadProgress.value = emptyMap()
                _downloadStates.value = emptyMap()
            } catch (_: Exception) {}
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DownloadManager? = null

        fun getInstance(context: Context): DownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManager(context).also { INSTANCE = it }
            }
        }

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}

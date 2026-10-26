package com.example.data.download

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.Track
import com.example.source.YouTubeMusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import java.util.concurrent.TimeUnit

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

    fun downloadTrack(track: Track, onComplete: ((Boolean) -> Unit)? = null) {
        if (_downloadProgress.value.containsKey(track.videoId)) return // Already in progress

        scope.launch {
            _downloadProgress.update { it + (track.videoId to 0.01f) }
            try {
                // 1. Resolve audio stream URL if not already available
                val streamUrl = track.directStreamUrl
                    ?: YouTubeMusicService.resolveStreamUrl(track.videoId)

                if (streamUrl.isNullOrBlank()) {
                    _downloadProgress.update { it - track.videoId }
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
                        withContext(Dispatchers.Main) { onComplete?.invoke(false) }
                        return@launch
                    }

                    val body = response.body ?: throw Exception("Empty response body")
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
                        withContext(Dispatchers.Main) { onComplete?.invoke(true) }
                    } else {
                        tempFile.delete()
                        _downloadProgress.update { it - track.videoId }
                        withContext(Dispatchers.Main) { onComplete?.invoke(false) }
                    }
                }
            } catch (e: Exception) {
                _downloadProgress.update { it - track.videoId }
                withContext(Dispatchers.Main) { onComplete?.invoke(false) }
            }
        }
    }

    fun deleteDownload(videoId: String) {
        scope.launch {
            try {
                val file = File(downloadsDir, "$videoId.m4a")
                if (file.exists()) {
                    file.delete()
                }
                trackDao.setDownloaded(videoId, false, null)
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
    }
}

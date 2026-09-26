package com.example.source

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class JioSaavnSource : MusicSource {

    override val sourceId: String = "JIOSAAVN"
    override val displayName: String = "JioSaavn"
    override val badgeLabel: String = "SAAVN_AUDIO"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // JioSaavn DES encryption key
    private val desKey = "38346585".toByteArray()

    override suspend fun search(query: String): List<UnifiedTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&n=20&p=1&q=$encoded&_marker=0&ctx=android"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@withContext emptyList()

                val list = mutableListOf<UnifiedTrack>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val id = item.optString("id")
                    if (id.isBlank()) continue

                    val songTitle = unescapeHtml(item.optString("song", "Unknown Track"))
                    val artists = unescapeHtml(item.optString("singers", item.optString("primary_artists", "Artist")))
                    val album = unescapeHtml(item.optString("album", ""))
                    val rawDuration = item.optString("duration", "0").toIntOrNull() ?: 0
                    val rawImage = item.optString("image", "")
                    val highResImage = rawImage.replace("150x150", "500x500")

                    val encryptedUrl = item.optString("encrypted_media_url", "")
                    val previewUrl = item.optString("media_preview_url", "")

                    val streamUrl = decryptMediaUrl(encryptedUrl) ?: previewUrl.ifBlank { null }

                    val mins = rawDuration / 60
                    val secs = rawDuration % 60
                    val durationFormatted = if (rawDuration > 0) String.format("%02d:%02d", mins, secs) else "STREAM"

                    list.add(
                        UnifiedTrack(
                            id = "js:$id",
                            sourceId = sourceId,
                            externalId = id,
                            title = songTitle,
                            artist = artists,
                            album = album,
                            thumbnailUrl = highResImage,
                            durationSec = rawDuration,
                            durationFormatted = durationFormatted,
                            genre = "JIOSAAVN HQ",
                            directStreamUrl = streamUrl
                        )
                    )
                }
                return@withContext list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun fetchTrending(): List<UnifiedTrack> = withContext(Dispatchers.IO) {
        search("top hindi english songs")
    }

    override suspend fun resolveStream(track: UnifiedTrack): ResolvedMedia? = withContext(Dispatchers.IO) {
        // If track already has direct stream URL
        if (track.sourceId == sourceId && !track.directStreamUrl.isNullOrBlank()) {
            return@withContext ResolvedMedia(
                track = track,
                playbackType = PlaybackType.DIRECT_AUDIO,
                streamUrl = track.directStreamUrl,
                youtubeVideoId = null,
                sourceName = displayName,
                resolutionNote = "JIOSAAVN 320KBPS DIRECT"
            )
        }

        // Query JioSaavn to find matching stream
        val searchResults = search("${track.title} ${track.artist}")
        val best = searchResults.firstOrNull { !it.directStreamUrl.isNullOrBlank() } ?: searchResults.firstOrNull()
        if (best != null && !best.directStreamUrl.isNullOrBlank()) {
            return@withContext ResolvedMedia(
                track = best,
                playbackType = PlaybackType.DIRECT_AUDIO,
                streamUrl = best.directStreamUrl,
                youtubeVideoId = null,
                sourceName = displayName,
                resolutionNote = "MATCHED VIA JIOSAAVN"
            )
        }
        null
    }

    override suspend fun fetchTrackById(id: String): UnifiedTrack? = withContext(Dispatchers.IO) {
        val cleanId = id.removePrefix("js:")
        try {
            val url = "https://www.jiosaavn.com/api.php?__call=song.getDetails&pids=$cleanId&_format=json&ctx=android"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val item = json.optJSONObject(cleanId) ?: return@withContext null

                val songTitle = unescapeHtml(item.optString("song", "Track"))
                val artists = unescapeHtml(item.optString("singers", "Artist"))
                val duration = item.optString("duration", "0").toIntOrNull() ?: 0
                val image = item.optString("image", "").replace("150x150", "500x500")
                val enc = item.optString("encrypted_media_url", "")
                val prev = item.optString("media_preview_url", "")
                val stream = decryptMediaUrl(enc) ?: prev

                UnifiedTrack(
                    id = "js:$cleanId",
                    sourceId = sourceId,
                    externalId = cleanId,
                    title = songTitle,
                    artist = artists,
                    thumbnailUrl = image,
                    durationSec = duration,
                    durationFormatted = String.format("%02d:%02d", duration / 60, duration % 60),
                    genre = "JIOSAAVN",
                    directStreamUrl = stream
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun decryptMediaUrl(encryptedUrl: String?): String? {
        if (encryptedUrl.isNullOrBlank()) return null
        return try {
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            val keySpec = SecretKeySpec(desKey, "DES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val decrypted = String(decryptedBytes, Charsets.UTF_8).trim()
            // Ensure valid audio url (can replace _96.mp4 with _320.mp4 if present)
            if (decrypted.endsWith(".mp4") || decrypted.endsWith(".m4a") || decrypted.startsWith("http")) {
                decrypted.replace("_96.mp4", "_320.mp4")
            } else {
                decrypted
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}

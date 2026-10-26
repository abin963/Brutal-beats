package com.example.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class LyricsResult(
    val title: String,
    val artist: String,
    val plainLyrics: String? = null,
    val syncedLyrics: List<LyricLine> = emptyList(),
    val isSynced: Boolean = syncedLyrics.isNotEmpty()
)

object LyricsService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val lrcPattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

    suspend fun fetchLyrics(
        trackTitle: String,
        artistName: String,
        durationSec: Int = 0
    ): LyricsResult? = withContext(Dispatchers.IO) {
        val cleanTitle = trackTitle
            .replace(Regex("\\(.*\\)"), "")
            .replace(Regex("\\[.*\\]"), "")
            .replace("Official Music Video", "", ignoreCase = true)
            .replace("Official Video", "", ignoreCase = true)
            .replace("Lyrics", "", ignoreCase = true)
            .replace("Audio", "", ignoreCase = true)
            .trim()

        val cleanArtist = artistName
            .replace(" - Topic", "")
            .replace("VEVO", "")
            .trim()

        if (cleanTitle.isBlank()) return@withContext null

        try {
            val encTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val encArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            val durParam = if (durationSec > 0) "&duration=$durationSec" else ""
            val url = "https://lrclib.net/api/get?track_name=$encTitle&artist_name=$encArtist$durParam"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NYXMusicPlayer/1.0 (https://github.com/example/nyx)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // Try search endpoint fallback if exact get returned 404
                    return@withContext searchLyricsFallback(cleanTitle, cleanArtist)
                }
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val syncedStr = json.optString("syncedLyrics", "").ifBlank { null }
                val plainStr = json.optString("plainLyrics", "").ifBlank { null }

                if (syncedStr == null && plainStr == null) {
                    return@withContext searchLyricsFallback(cleanTitle, cleanArtist)
                }

                val parsedSynced = syncedStr?.let { parseLrc(it) } ?: emptyList()
                return@withContext LyricsResult(
                    title = cleanTitle,
                    artist = cleanArtist,
                    plainLyrics = plainStr,
                    syncedLyrics = parsedSynced
                )
            }
        } catch (_: Exception) {
            searchLyricsFallback(cleanTitle, cleanArtist)
        }
    }

    private fun searchLyricsFallback(title: String, artist: String): LyricsResult? {
        try {
            val query = URLEncoder.encode("$title $artist", "UTF-8")
            val url = "https://lrclib.net/api/search?q=$query"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NYXMusicPlayer/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val array = org.json.JSONArray(body)
                if (array.length() == 0) return null

                val first = array.getJSONObject(0)
                val syncedStr = first.optString("syncedLyrics", "").ifBlank { null }
                val plainStr = first.optString("plainLyrics", "").ifBlank { null }
                val parsedSynced = syncedStr?.let { parseLrc(it) } ?: emptyList()

                return LyricsResult(
                    title = title,
                    artist = artist,
                    plainLyrics = plainStr,
                    syncedLyrics = parsedSynced
                )
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        for (line in lrcText.lines()) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue
            val matcher = lrcPattern.matcher(trimmed)
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val msStr = matcher.group(3) ?: "0"
                val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                val timestampMs = (min * 60 + sec) * 1000 + ms
                val text = matcher.group(4)?.trim() ?: ""
                lines.add(LyricLine(timestampMs, text))
            }
        }
        return lines.sortedBy { it.timestampMs }
    }
}

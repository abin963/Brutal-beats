package com.example.source

import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * YouTube Music InnerTube client adapted from Metrolist.
 * Provides rich YouTube Music catalog search, artist/album metadata,
 * related tracks/radio continuation, and direct audio stream resolution.
 */
object YouTubeMusicService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    suspend fun searchSongs(query: String): List<Track> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240101.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("query", trimmed)
                // Filter specifically for Songs
                put("params", "Eg-KAQwIABAAGAAgACgAMABqChAKEAMQBBAFEAo=")
            }

            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/search")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Referer", "https://music.youtube.com/")
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                return@withContext parseInnerTubeSearch(body)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null

        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID_MUSIC")
                        put("clientVersion", "6.41.52")
                        put("androidSdkVersion", 34)
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("videoId", videoId)
            }

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player")
                .header("User-Agent", "com.google.android.apps.youtube.music/6.41.52 (Linux; U; Android 14) gzip")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                val streamingData = json.optJSONObject("streamingData") ?: return@withContext null
                val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats") ?: JSONArray()

                var bestAudioUrl: String? = null
                var highestBitrate = 0

                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.getJSONObject(i)
                    val mimeType = format.optString("mimeType", "")
                    if (mimeType.startsWith("audio/")) {
                        val directUrl = format.optString("url", "")
                        val bitrate = format.optInt("bitrate", 0)
                        if (directUrl.isNotBlank() && bitrate > highestBitrate) {
                            highestBitrate = bitrate
                            bestAudioUrl = directUrl
                        }
                    }
                }
                return@withContext bestAudioUrl
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchRadioTracks(videoId: String): List<Track> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext emptyList()

        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240101.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("videoId", videoId)
            }

            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/next")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Referer", "https://music.youtube.com/")
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                return@withContext parseInnerTubeNext(body)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseInnerTubeSearch(jsonStr: String): List<Track> {
        val results = mutableListOf<Track>()
        try {
            val root = JSONObject(jsonStr)
            val contents = root.optJSONObject("contents")
                ?.optJSONObject("tabbedSearchResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            for (c in 0 until contents.length()) {
                val section = contents.getJSONObject(c).optJSONObject("musicShelfRenderer") ?: continue
                val items = section.optJSONArray("contents") ?: continue

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i).optJSONObject("musicResponsiveListItemRenderer") ?: continue
                    val flexColumns = item.optJSONArray("flexColumns") ?: continue
                    if (flexColumns.length() < 2) continue

                    // Column 0: Title and Video ID
                    val col0 = flexColumns.getJSONObject(0)
                        .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                    val titleRuns = col0?.optJSONArray("runs") ?: continue
                    val title = titleRuns.optJSONObject(0)?.optString("text") ?: continue

                    val playNav = item.optJSONObject("overlay")
                        ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                        ?.optJSONObject("content")
                        ?.optJSONObject("musicPlayButtonRenderer")
                        ?.optJSONObject("playNavigationEndpoint")
                    val videoId = playNav?.optJSONObject("watchEndpoint")?.optString("videoId")
                        ?: item.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
                        ?: continue

                    if (videoId.length != 11) continue

                    // Column 1: Artist, Album, Duration
                    val col1 = flexColumns.getJSONObject(1)
                        .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                    val infoRuns = col1?.optJSONArray("runs") ?: JSONArray()
                    var artist = "Unknown Artist"
                    var duration = "3:30"

                    if (infoRuns.length() > 0) {
                        artist = infoRuns.optJSONObject(0)?.optString("text") ?: "Artist"
                    }
                    if (infoRuns.length() >= 3) {
                        val lastRun = infoRuns.optJSONObject(infoRuns.length() - 1)?.optString("text") ?: ""
                        if (lastRun.contains(":")) {
                            duration = lastRun
                        }
                    }

                    // Thumbnail
                    val thumbs = item.optJSONObject("thumbnail")
                        ?.optJSONObject("musicThumbnailRenderer")
                        ?.optJSONObject("thumbnail")
                        ?.optJSONArray("thumbnails")
                    val rawThumb = if (thumbs != null && thumbs.length() > 0) {
                        thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                    } else "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                    val highResThumb = rawThumb.replace(Regex("w\\d+-h\\d+"), "w544-h544")

                    results.add(
                        Track(
                            videoId = videoId,
                            title = title,
                            artist = artist,
                            thumbnailUrl = highResThumb,
                            duration = duration,
                            genre = "YOUTUBE MUSIC",
                            sourceId = "YOUTUBE",
                            ytSourceLabel = "YT_MUSIC"
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return results
    }

    private fun parseInnerTubeNext(jsonStr: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val root = JSONObject(jsonStr)
            val tabs = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                ?.optJSONObject("tabbedRenderer")
                ?.optJSONObject("watchNextTabbedResultsRenderer")
                ?.optJSONArray("tabs") ?: return emptyList()

            for (t in 0 until tabs.length()) {
                val tab = tabs.getJSONObject(t).optJSONObject("tabRenderer") ?: continue
                val queueRenderer = tab.optJSONObject("content")
                    ?.optJSONObject("musicQueueRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("playlistPanelRenderer") ?: continue

                val contents = queueRenderer.optJSONArray("contents") ?: continue
                for (i in 0 until contents.length()) {
                    val item = contents.getJSONObject(i).optJSONObject("playlistPanelVideoRenderer") ?: continue
                    val videoId = item.optString("videoId")
                    if (videoId.length != 11) continue

                    val title = item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: continue
                    val artist = item.optJSONObject("longBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: item.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        ?: "Artist"
                    val duration = item.optJSONObject("lengthText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "3:30"

                    val thumbs = item.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                    val rawThumb = if (thumbs != null && thumbs.length() > 0) {
                        thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                    } else "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                    tracks.add(
                        Track(
                            videoId = videoId,
                            title = title,
                            artist = artist,
                            thumbnailUrl = rawThumb.replace(Regex("w\\d+-h\\d+"), "w544-h544"),
                            duration = duration,
                            genre = "YOUTUBE MUSIC",
                            sourceId = "YOUTUBE",
                            ytSourceLabel = "YT_MUSIC"
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return tracks
    }
}

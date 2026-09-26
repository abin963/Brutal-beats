package com.example.data.remote

import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object YouTubeSearchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(body)
                if (jsonArray.length() > 1) {
                    val suggestionsArray = jsonArray.getJSONArray(1)
                    val list = mutableListOf<String>()
                    for (i in 0 until suggestionsArray.length().coerceAtMost(8)) {
                        list.add(suggestionsArray.getString(i))
                    }
                    return@withContext list
                }
            }
        } catch (_: Exception) {}
        emptyList()
    }

    suspend fun searchYouTube(query: String, genreLabel: String = "YOUTUBE"): List<Track> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        // Check if query is a direct URL or 11-char ID
        val directId = extractVideoId(trimmed)
        if (directId != null) {
            val directTrack = fetchTrackByVideoId(directId)
            if (directTrack != null) {
                return@withContext listOf(directTrack)
            }
        }

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val html = response.body?.string() ?: return@withContext emptyList()
                return@withContext parseYouTubeInitialData(html, genreLabel)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchTrackByVideoId(videoId: String): Track? = withContext(Dispatchers.IO) {
        try {
            val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Track(
                        videoId = videoId,
                        title = "YOUTUBE TRACK: $videoId",
                        artist = "YOUTUBE ARTIST",
                        thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                        duration = "STREAM",
                        genre = "DIRECT LINK",
                        ytSourceLabel = "DIRECT_YT"
                    )
                }
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val title = json.optString("title", "YOUTUBE AUDIO")
                val author = json.optString("author_name", "YOUTUBE CREATOR")
                val thumb = json.optString("thumbnail_url", "https://img.youtube.com/vi/$videoId/hqdefault.jpg")

                return@withContext Track(
                    videoId = videoId,
                    title = title,
                    artist = author,
                    thumbnailUrl = thumb,
                    duration = "STREAM",
                    genre = "DIRECT LINK",
                    ytSourceLabel = "DIRECT_YT"
                )
            }
        } catch (_: Exception) {
            Track(
                videoId = videoId,
                title = "YOUTUBE STREAM ($videoId)",
                artist = "YOUTUBE",
                thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                duration = "STREAM",
                genre = "DIRECT LINK",
                ytSourceLabel = "DIRECT_YT"
            )
        }
    }

    private fun parseYouTubeInitialData(html: String, genreLabel: String): List<Track> {
        val tracks = mutableListOf<Track>()
        val seenIds = mutableSetOf<String>()

        try {
            val pattern = Pattern.compile("ytInitialData\\s*=\\s*(\\{.*?\\});</script>", Pattern.DOTALL)
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val jsonStr = matcher.group(1)
                if (jsonStr != null) {
                    val root = JSONObject(jsonStr)
                    val contents = root.optJSONObject("contents")
                        ?.optJSONObject("twoColumnSearchResultsRenderer")
                        ?.optJSONObject("primaryContents")
                        ?.optJSONObject("sectionListRenderer")
                        ?.optJSONArray("contents")

                    if (contents != null) {
                        for (i in 0 until contents.length()) {
                            val sec = contents.optJSONObject(i) ?: continue
                            val itemSection = sec.optJSONObject("itemSectionRenderer") ?: continue
                            val itemContents = itemSection.optJSONArray("contents") ?: continue

                            for (j in 0 until itemContents.length()) {
                                val item = itemContents.optJSONObject(j) ?: continue
                                val v = item.optJSONObject("videoRenderer") ?: continue
                                val videoId = v.optString("videoId")
                                if (videoId.length != 11 || videoId in seenIds) continue

                                val titleObj = v.optJSONObject("title")
                                val runs = titleObj?.optJSONArray("runs")
                                val title = if (runs != null && runs.length() > 0) {
                                    runs.optJSONObject(0)?.optString("text") ?: ""
                                } else {
                                    titleObj?.optString("simpleText") ?: "YouTube Audio"
                                }

                                val ownerObj = v.optJSONObject("ownerText")
                                val ownerRuns = ownerObj?.optJSONArray("runs")
                                val artist = if (ownerRuns != null && ownerRuns.length() > 0) {
                                    ownerRuns.optJSONObject(0)?.optString("text") ?: "YouTube Creator"
                                } else {
                                    ownerObj?.optString("simpleText") ?: "YouTube Creator"
                                }

                                val lengthObj = v.optJSONObject("lengthText")
                                val duration = lengthObj?.optString("simpleText", "LIVE") ?: "LIVE"

                                seenIds.add(videoId)
                                tracks.add(
                                    Track(
                                        videoId = videoId,
                                        title = title.trim(),
                                        artist = artist.trim(),
                                        thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                                        duration = duration,
                                        genre = genreLabel,
                                        ytSourceLabel = "LIVE_SEARCH"
                                    )
                                )
                                if (tracks.size >= 25) break
                            }
                            if (tracks.size >= 25) break
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback fast regex scan over videoRenderer blocks if full JSON parsing failed
        if (tracks.isEmpty()) {
            val videoBlockRegex = Pattern.compile(
                "\"videoRenderer\":\\{\"videoId\":\"([a-zA-Z0-9_-]{11})\".*?\"title\":\\{(?:\"runs\":\\[\\{\"text\":\"(.*?)\"\\}\\]|\"simpleText\":\"(.*?)\"\\).*?\"ownerText\":\\{\"runs\":\\[\\{\"text\":\"(.*?)\"",
                Pattern.DOTALL
            )
            val matcher = videoBlockRegex.matcher(html)
            while (matcher.find() && tracks.size < 20) {
                val vid = matcher.group(1) ?: continue
                if (vid in seenIds) continue
                seenIds.add(vid)

                val rawTitle = matcher.group(2) ?: matcher.group(3) ?: "YouTube Track"
                val rawArtist = matcher.group(4) ?: "YouTube Creator"

                tracks.add(
                    Track(
                        videoId = vid,
                        title = unescapeUnicode(rawTitle).replace("\\\"", "\"").take(70),
                        artist = unescapeUnicode(rawArtist).replace("\\\"", "\"").take(40),
                        thumbnailUrl = "https://img.youtube.com/vi/$vid/hqdefault.jpg",
                        duration = "STREAM",
                        genre = genreLabel,
                        ytSourceLabel = "LIVE_SEARCH"
                    )
                )
            }
        }

        return tracks
    }

    fun extractVideoId(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return trimmed
        }
        val urlRegex = Regex("(?:youtube\\.com/(?:[^/]+/.*|(?:v|e(?:mbed)?)|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})")
        val match = urlRegex.find(trimmed)
        return match?.groupValues?.getOrNull(1)
    }

    private fun unescapeUnicode(str: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < str.length) {
            val c = str[i]
            if (c == '\\' && i + 1 < str.length && str[i + 1] == 'u' && i + 5 < str.length) {
                try {
                    val code = str.substring(i + 2, i + 6).toInt(16)
                    sb.append(code.toChar())
                    i += 6
                    continue
                } catch (_: Exception) {}
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }
}

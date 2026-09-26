package com.example.source

import com.example.data.remote.YouTubeSearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * YouTubeExtractor: Extracts audio streams, metadata, and embeds for YouTube video streams.
 */
object YouTubeExtractor {

    suspend fun extractTrack(queryOrUrl: String): UnifiedTrack? = withContext(Dispatchers.IO) {
        val videoId = YouTubeSearchService.extractVideoId(queryOrUrl)
        if (videoId != null) {
            val track = YouTubeSearchService.fetchTrackByVideoId(videoId) ?: return@withContext null
            return@withContext UnifiedTrack(
                id = "yt:$videoId",
                sourceId = "YOUTUBE",
                externalId = videoId,
                title = track.title,
                artist = track.artist,
                thumbnailUrl = track.thumbnailUrl,
                durationFormatted = track.duration,
                genre = track.genre
            )
        }
        val search = YouTubeSearchService.searchYouTube(queryOrUrl, "YOUTUBE")
        val first = search.firstOrNull() ?: return@withContext null
        UnifiedTrack(
            id = "yt:${first.videoId}",
            sourceId = "YOUTUBE",
            externalId = first.videoId,
            title = first.title,
            artist = first.artist,
            thumbnailUrl = first.thumbnailUrl,
            durationFormatted = first.duration,
            genre = first.genre
        )
    }

    fun getEmbedHtmlUrl(videoId: String): String {
        return "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&enablejsapi=1&playsinline=1&controls=1&rel=0&modestbranding=1"
    }
}

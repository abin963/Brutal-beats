package com.example.source

import com.example.data.remote.YouTubeSearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubeSource : MusicSource {

    override val sourceId: String = "YOUTUBE"
    override val displayName: String = "YouTube Music"
    override val badgeLabel: String = "YT_AUDIO"

    override suspend fun search(query: String): List<UnifiedTrack> = withContext(Dispatchers.IO) {
        val tracks = YouTubeSearchService.searchYouTube(query, "YOUTUBE")
        tracks.map { track ->
            UnifiedTrack(
                id = "yt:${track.videoId}",
                sourceId = sourceId,
                externalId = track.videoId,
                title = track.title,
                artist = track.artist,
                thumbnailUrl = track.thumbnailUrl,
                durationFormatted = track.duration,
                genre = track.genre,
                directStreamUrl = null
            )
        }
    }

    override suspend fun fetchTrending(): List<UnifiedTrack> = withContext(Dispatchers.IO) {
        search("trending music official")
    }

    override suspend fun resolveStream(track: UnifiedTrack): ResolvedMedia? = withContext(Dispatchers.IO) {
        val videoId = if (track.sourceId == sourceId) {
            track.externalId
        } else {
            // Find corresponding YouTube track
            val searchResults = search("${track.title} ${track.artist}")
            searchResults.firstOrNull()?.externalId
        } ?: return@withContext null

        ResolvedMedia(
            track = track.copy(externalId = videoId),
            playbackType = PlaybackType.YOUTUBE_EMBED,
            streamUrl = null,
            youtubeVideoId = videoId,
            sourceName = displayName,
            resolutionNote = "DIRECT YOUTUBE STREAM"
        )
    }

    override suspend fun fetchTrackById(id: String): UnifiedTrack? = withContext(Dispatchers.IO) {
        val cleanId = id.removePrefix("yt:")
        val track = YouTubeSearchService.fetchTrackByVideoId(cleanId) ?: return@withContext null
        UnifiedTrack(
            id = "yt:${track.videoId}",
            sourceId = sourceId,
            externalId = track.videoId,
            title = track.title,
            artist = track.artist,
            thumbnailUrl = track.thumbnailUrl,
            durationFormatted = track.duration,
            genre = track.genre
        )
    }
}

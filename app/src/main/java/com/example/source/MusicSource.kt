package com.example.source

enum class PlaybackType {
    YOUTUBE_EMBED,
    DIRECT_AUDIO
}

data class UnifiedTrack(
    val id: String,                    // e.g. "yt:A2VpR8HahKc" or "js:_WBx654g"
    val sourceId: String,              // "YOUTUBE" or "JIOSAAVN"
    val externalId: String,            // YouTube videoId or JioSaavn id
    val title: String,
    val artist: String,
    val album: String = "",
    val thumbnailUrl: String,
    val durationSec: Int = 0,
    val durationFormatted: String = "LIVE",
    val genre: String = "RAW BEATS",
    val directStreamUrl: String? = null,
    val isFavorite: Boolean = false,
    val matchConfidence: Float = 1.0f
)

data class ResolvedMedia(
    val track: UnifiedTrack,
    val playbackType: PlaybackType,
    val streamUrl: String?,
    val youtubeVideoId: String?,
    val sourceName: String,
    val resolutionNote: String = ""
)

interface MusicSource {
    val sourceId: String
    val displayName: String
    val badgeLabel: String

    suspend fun search(query: String): List<UnifiedTrack>
    suspend fun fetchTrending(): List<UnifiedTrack>
    suspend fun resolveStream(track: UnifiedTrack): ResolvedMedia?
    suspend fun fetchTrackById(id: String): UnifiedTrack?
}

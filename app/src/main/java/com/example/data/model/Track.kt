package com.example.data.model

import com.example.source.UnifiedTrack

data class Track(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val duration: String,
    val genre: String = "RAW BEATS",
    val isFavorite: Boolean = false,
    val ytSourceLabel: String = "YOUTUBE",
    val sourceId: String = "YOUTUBE",
    val directStreamUrl: String? = null,
    val matchConfidence: Float = 1.0f
) {
    val embedUrl: String
        get() = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&enablejsapi=1&playsinline=1&controls=1&rel=0&modestbranding=1"

    fun toUnifiedTrack(): UnifiedTrack = UnifiedTrack(
        id = if (sourceId == "JIOSAAVN") "js:$videoId" else "yt:$videoId",
        sourceId = sourceId,
        externalId = videoId,
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        durationFormatted = duration,
        genre = genre,
        directStreamUrl = directStreamUrl,
        isFavorite = isFavorite,
        matchConfidence = matchConfidence
    )

    companion object {
        fun fromUnifiedTrack(unified: UnifiedTrack, isFav: Boolean = unified.isFavorite): Track = Track(
            videoId = unified.externalId,
            title = unified.title,
            artist = unified.artist,
            thumbnailUrl = unified.thumbnailUrl,
            duration = unified.durationFormatted,
            genre = unified.genre,
            isFavorite = isFav,
            ytSourceLabel = unified.sourceId,
            sourceId = unified.sourceId,
            directStreamUrl = unified.directStreamUrl,
            matchConfidence = unified.matchConfidence
        )
    }
}

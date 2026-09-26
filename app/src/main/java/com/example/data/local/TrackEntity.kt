package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val duration: String,
    val genre: String = "TECHNO",
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val playCount: Int = 0,
    val addedToFavoritesTimestamp: Long = 0L,
    val sourceId: String = "YOUTUBE",
    val directStreamUrl: String? = null
) {
    fun toTrack(): Track = Track(
        videoId = videoId,
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        genre = genre,
        isFavorite = isFavorite,
        sourceId = sourceId,
        ytSourceLabel = sourceId,
        directStreamUrl = directStreamUrl
    )

    companion object {
        fun fromTrack(track: Track, isFav: Boolean = track.isFavorite): TrackEntity = TrackEntity(
            videoId = track.videoId,
            title = track.title,
            artist = track.artist,
            thumbnailUrl = track.thumbnailUrl,
            duration = track.duration,
            genre = track.genre,
            isFavorite = isFav,
            sourceId = track.sourceId,
            directStreamUrl = track.directStreamUrl
        )
    }
}

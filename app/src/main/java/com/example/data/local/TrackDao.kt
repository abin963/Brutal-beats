package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY addedToFavoritesTimestamp DESC")
    fun getFavorites(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE videoId = :videoId LIMIT 1")
    suspend fun getTrack(videoId: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE videoId = :videoId LIMIT 1")
    fun getTrackFlow(videoId: String): Flow<TrackEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Query("UPDATE tracks SET isFavorite = :isFavorite, addedToFavoritesTimestamp = :timestamp WHERE videoId = :videoId")
    suspend fun setFavorite(videoId: String, isFavorite: Boolean, timestamp: Long)

    @Query("UPDATE tracks SET lastPlayedTimestamp = :timestamp, playCount = playCount + 1 WHERE videoId = :videoId")
    suspend fun recordPlay(videoId: String, timestamp: Long)

    @Query("UPDATE tracks SET lastPlayedTimestamp = 0 WHERE videoId = :videoId")
    suspend fun removeFromHistory(videoId: String)

    @Query("UPDATE tracks SET lastPlayedTimestamp = 0")
    suspend fun clearHistory()
}

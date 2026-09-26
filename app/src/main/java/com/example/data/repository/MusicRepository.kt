package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.TrackEntity
import com.example.data.model.Track
import com.example.data.remote.GenrePresets
import com.example.source.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(private val database: AppDatabase) {

    private val trackDao = database.trackDao()
    private val playlistDao = database.playlistDao()

    val favoritesFlow: Flow<List<Track>> = trackDao.getFavorites().map { list ->
        list.map { it.toTrack() }
    }

    val historyFlow: Flow<List<Track>> = trackDao.getRecentlyPlayed(50).map { list ->
        list.map { it.toTrack() }
    }

    val playlistsFlow: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun fetchTrendingTracks(sourceMode: PreferredSourceMode = PreferredSourceMode.AUTO): List<Track> = withContext(Dispatchers.IO) {
        val tracks = when (sourceMode) {
            PreferredSourceMode.JIOSAAVN -> SourceRegistry.getJioSaavnSource().fetchTrending()
            PreferredSourceMode.YOUTUBE -> SourceRegistry.getYouTubeSource().fetchTrending()
            PreferredSourceMode.AUTO -> {
                val yt = SourceRegistry.getYouTubeSource().fetchTrending()
                val saavn = SourceRegistry.getJioSaavnSource().fetchTrending()
                // Interleave or combine both sources
                (yt.take(12) + saavn.take(12)).distinctBy { it.id }
            }
        }
        val domainTracks = tracks.map { Track.fromUnifiedTrack(it) }
        if (domainTracks.isNotEmpty()) {
            trackDao.insertTracks(domainTracks.map { TrackEntity.fromTrack(it) })
        }
        domainTracks
    }

    suspend fun fetchGenreTracks(genreName: String, sourceMode: PreferredSourceMode = PreferredSourceMode.AUTO): List<Track> = withContext(Dispatchers.IO) {
        val preset = GenrePresets.GENRES.find { it.name.equals(genreName, ignoreCase = true) }
        val query = preset?.searchQuery ?: "$genreName music"

        val unifiedList = when (sourceMode) {
            PreferredSourceMode.JIOSAAVN -> SourceRegistry.getJioSaavnSource().search(query)
            PreferredSourceMode.YOUTUBE -> SourceRegistry.getYouTubeSource().search(query)
            PreferredSourceMode.AUTO -> {
                val yt = SourceRegistry.getYouTubeSource().search(query)
                val js = SourceRegistry.getJioSaavnSource().search(query)
                (yt.take(12) + js.take(12)).distinctBy { it.id }
            }
        }

        val domainTracks = unifiedList.map { Track.fromUnifiedTrack(it) }
        if (domainTracks.isNotEmpty()) {
            trackDao.insertTracks(domainTracks.map { TrackEntity.fromTrack(it) })
        }
        domainTracks
    }

    suspend fun search(query: String, sourceMode: PreferredSourceMode = PreferredSourceMode.AUTO): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val results = when (sourceMode) {
            PreferredSourceMode.YOUTUBE -> SourceRegistry.getYouTubeSource().search(query)
            PreferredSourceMode.JIOSAAVN -> SourceRegistry.getJioSaavnSource().search(query)
            PreferredSourceMode.AUTO -> {
                val ytResults = SourceRegistry.getYouTubeSource().search(query)
                val saavnResults = SourceRegistry.getJioSaavnSource().search(query)
                (ytResults + saavnResults).distinctBy { "${it.title.lowercase().trim()}_${it.artist.lowercase().trim()}" }
            }
        }

        val domainTracks = results.map { Track.fromUnifiedTrack(it) }
        if (domainTracks.isNotEmpty()) {
            trackDao.insertTracks(domainTracks.map { TrackEntity.fromTrack(it) })
        }
        domainTracks
    }

    suspend fun resolveMedia(track: Track, preferredMode: PreferredSourceMode): ResolvedMedia = withContext(Dispatchers.IO) {
        SourceResolver.resolve(track.toUnifiedTrack(), preferredMode)
    }

    suspend fun fetchDirectTrack(urlOrId: String): Track? = withContext(Dispatchers.IO) {
        val ytSource = SourceRegistry.getYouTubeSource()
        val unified = ytSource.fetchTrackById(urlOrId)
        if (unified != null) {
            val track = Track.fromUnifiedTrack(unified)
            trackDao.upsertTrack(TrackEntity.fromTrack(track))
            return@withContext track
        }
        null
    }

    suspend fun toggleFavorite(track: Track): Boolean = withContext(Dispatchers.IO) {
        val current = trackDao.getTrack(track.videoId)
        val newFav = if (current != null) !current.isFavorite else true
        val entity = current?.copy(
            isFavorite = newFav,
            addedToFavoritesTimestamp = if (newFav) System.currentTimeMillis() else 0L
        ) ?: TrackEntity.fromTrack(track, isFav = true).copy(
            addedToFavoritesTimestamp = System.currentTimeMillis()
        )
        trackDao.upsertTrack(entity)
        newFav
    }

    suspend fun isFavorite(videoId: String): Boolean = withContext(Dispatchers.IO) {
        trackDao.getTrack(videoId)?.isFavorite == true
    }

    suspend fun recordPlayed(track: Track) = withContext(Dispatchers.IO) {
        val existing = trackDao.getTrack(track.videoId)
        val timestamp = System.currentTimeMillis()
        if (existing == null) {
            val entity = TrackEntity.fromTrack(track).copy(
                lastPlayedTimestamp = timestamp,
                playCount = 1
            )
            trackDao.upsertTrack(entity)
        } else {
            trackDao.recordPlay(track.videoId, timestamp)
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        trackDao.clearHistory()
    }

    suspend fun removeFromHistory(videoId: String) = withContext(Dispatchers.IO) {
        trackDao.removeFromHistory(videoId)
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name, description = description))
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: Track) = withContext(Dispatchers.IO) {
        val existing = trackDao.getTrack(track.videoId)
        if (existing == null) {
            trackDao.upsertTrack(TrackEntity.fromTrack(track))
        }
        playlistDao.addTrackToPlaylist(
            PlaylistTrackCrossRef(playlistId = playlistId, trackVideoId = track.videoId)
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, videoId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, videoId)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return playlistDao.getTracksForPlaylist(playlistId).map { list ->
            list.map { it.toTrack() }
        }
    }
}

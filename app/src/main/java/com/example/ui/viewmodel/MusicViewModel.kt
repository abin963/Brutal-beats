package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import coil.request.ImageRequest
import com.example.data.local.AppDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.model.Track
import com.example.data.repository.MusicRepository
import com.example.data.remote.YouTubeSearchService
import com.example.source.*
import com.example.ui.components.ThumbnailUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MainTab {
    HOME,
    EXPLORE,
    LIBRARY,
    SEARCH,
    HISTORY;

    companion object {
        val DISCOVER: MainTab get() = EXPLORE
    }
}

data class PlayerUiState(
    val currentTrack: Track? = null,
    val resolvedMedia: ResolvedMedia? = null,
    val playbackType: PlaybackType = PlaybackType.YOUTUBE_EMBED,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionSec: Float = 0f,
    val totalDurationSec: Float = 0f,
    val volume: Float = 100f,
    val isMuted: Boolean = false,
    val isLooping: Boolean = false,
    val isShuffling: Boolean = false,
    val isAutoplayEnabled: Boolean = true,
    val seekTargetSec: Float? = null,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = 0,
    val isPlayerExpanded: Boolean = false,
    val isQueueVisible: Boolean = false,
    val isVideoMode: Boolean = false,
    val preferredSourceMode: PreferredSourceMode = PreferredSourceMode.AUTO,
    val isRadioActive: Boolean = false,
    val radioSeedTrack: Track? = null
)

data class RadioState(
    val isRadioActive: Boolean = false,
    val seedTrack: Track? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val radioHistory: List<String> = emptyList()
)

data class BrutalUiState(
    val activeTab: MainTab = MainTab.HOME,
    val searchQuery: String = "",
    val searchResults: List<Track> = emptyList(),
    val isSearching: Boolean = false,
    val searchSuggestions: List<String> = emptyList(),
    val selectedGenre: String = "TRENDING",
    val genreTracks: List<Track> = emptyList(),
    val isLoadingGenre: Boolean = false,
    val directUrlInput: String = "",
    val isFetchingDirectLink: Boolean = false,
    val statusMessage: String? = null,
    val selectedPlaylist: PlaylistEntity? = null,
    val selectedPlaylistTracks: List<Track> = emptyList(),
    val preferredSourceMode: PreferredSourceMode = PreferredSourceMode.AUTO
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(AppDatabase.getInstance(application))
    private val audioPlaybackManager = AudioPlaybackManager(application)
    private val prefs = application.getSharedPreferences("brutal_beats_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTOPLAY = "is_autoplay_enabled"
    }

    private val _playerState = MutableStateFlow(
        PlayerUiState(isAutoplayEnabled = prefs.getBoolean(KEY_AUTOPLAY, true))
    )
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    private val _uiState = MutableStateFlow(BrutalUiState())
    val uiState: StateFlow<BrutalUiState> = _uiState.asStateFlow()

    val favorites: StateFlow<List<Track>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<Track>> = repository.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _recommendations = MutableStateFlow<List<Track>>(emptyList())
    val recommendations: StateFlow<List<Track>> = _recommendations.asStateFlow()

    private val _isLoadingRecommendations = MutableStateFlow(false)
    val isLoadingRecommendations: StateFlow<Boolean> = _isLoadingRecommendations.asStateFlow()

    private val _radioState = MutableStateFlow(RadioState())
    val radioState: StateFlow<RadioState> = _radioState.asStateFlow()

    private val recommendationsCache = mutableMapOf<String, List<Track>>()
    private val recentlyAutoplayedIds = LinkedHashSet<String>()

    private var radioFetchJob: Job? = null
    private var recommendationJob: Job? = null
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null
    private var genreJob: Job? = null
    private var autoplayJob: Job? = null

    private var lastSeekTimestamp = 0L
    private var targetSeekSec = -1f

    private var lastCompletedTrackId: String? = null
    private var lastCompletedTimestamp = 0L
    private var consecutiveFailures = 0
    private var preloadedMedia: ResolvedMedia? = null
    private var isAdvancingAutoplay = false

    init {
        // Collect direct audio progress from AudioPlaybackManager
        viewModelScope.launch {
            audioPlaybackManager.currentPosition.collect { pos ->
                if (_playerState.value.playbackType == PlaybackType.DIRECT_AUDIO && _playerState.value.isPlaying) {
                    val now = System.currentTimeMillis()
                    if (now - lastSeekTimestamp < 800) {
                        if (targetSeekSec >= 0f && kotlin.math.abs(pos - targetSeekSec) > 2f) {
                            return@collect
                        }
                    }
                    targetSeekSec = -1f
                    _playerState.update { current ->
                        val prev = current.currentPositionSec
                        val newPos = if (pos >= prev || (prev - pos > 4f)) pos else prev
                        current.copy(currentPositionSec = newPos, isBuffering = false)
                    }
                }
            }
        }
        viewModelScope.launch {
            audioPlaybackManager.duration.collect { dur ->
                if (_playerState.value.playbackType == PlaybackType.DIRECT_AUDIO && dur > 0) {
                    _playerState.update { it.copy(totalDurationSec = dur) }
                }
            }
        }
        viewModelScope.launch {
            audioPlaybackManager.isDirectPlaying.collect { playing ->
                if (_playerState.value.playbackType == PlaybackType.DIRECT_AUDIO) {
                    if (playing) consecutiveFailures = 0
                    _playerState.update { it.copy(isPlaying = playing, isBuffering = false) }
                }
            }
        }

        // Initialize feed
        selectGenre("TRENDING")
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(activeTab = tab, selectedPlaylist = null) }
    }

    fun setPreferredSource(mode: PreferredSourceMode) {
        _uiState.update { it.copy(preferredSourceMode = mode) }
        _playerState.update { it.copy(preferredSourceMode = mode) }
        showMessage("SOURCE MODE: ${mode.name}")

        // Refresh genre feed for new source mode
        selectGenre(_uiState.value.selectedGenre)

        // If currently playing, re-resolve current track with the new source
        _playerState.value.currentTrack?.let { track ->
            playTrack(track)
        }
    }

    fun toggleAutoplay() {
        val newState = !_playerState.value.isAutoplayEnabled
        _playerState.update { it.copy(isAutoplayEnabled = newState) }
        prefs.edit().putBoolean(KEY_AUTOPLAY, newState).apply()
        showMessage(if (newState) "AUTOPLAY: ON" else "AUTOPLAY: OFF")
    }

    fun selectGenre(genre: String) {
        _uiState.update { it.copy(selectedGenre = genre, isLoadingGenre = true) }
        genreJob?.cancel()
        genreJob = viewModelScope.launch {
            val tracks = if (genre.equals("TRENDING", ignoreCase = true)) {
                repository.fetchTrendingTracks(_uiState.value.preferredSourceMode)
            } else {
                repository.fetchGenreTracks(genre, _uiState.value.preferredSourceMode)
            }
            _uiState.update { it.copy(genreTracks = tracks, isLoadingGenre = false) }
        }
    }

    fun updateDirectUrlInput(input: String) {
        _uiState.update { it.copy(directUrlInput = input) }
    }

    fun playDirectLink(urlOrId: String) {
        val trimmed = urlOrId.trim()
        if (trimmed.isBlank()) return

        _uiState.update { it.copy(isFetchingDirectLink = true) }
        viewModelScope.launch {
            val track = repository.fetchDirectTrack(trimmed)
            _uiState.update { it.copy(isFetchingDirectLink = false, directUrlInput = "") }
            if (track != null) {
                playTrack(track)
                showMessage("RESOLVED: ${track.title.take(20)}")
            } else {
                showMessage("INVALID LINK OR STREAM ID")
            }
        }
    }

    private fun parseDurationSeconds(durationStr: String?): Float {
        if (durationStr.isNullOrBlank()) return 0f
        val parts = durationStr.trim().split(":")
        return when (parts.size) {
            2 -> (parts[0].toIntOrNull() ?: 0) * 60f + (parts[1].toIntOrNull() ?: 0)
            3 -> (parts[0].toIntOrNull() ?: 0) * 3600f + (parts[1].toIntOrNull() ?: 0) * 60f + (parts[2].toIntOrNull() ?: 0)
            else -> 0f
        }
    }

    fun loadRecommendations(track: Track) {
        val cached = recommendationsCache[track.videoId]
        if (cached != null && cached.isNotEmpty()) {
            _recommendations.value = cached
            _isLoadingRecommendations.value = false
            preloadNextTrackFromList(cached, track.videoId)
            return
        }

        recommendationJob?.cancel()
        _isLoadingRecommendations.value = true
        recommendationJob = viewModelScope.launch {
            val recs = repository.fetchRecommendedTracks(track, _playerState.value.preferredSourceMode)
            recommendationsCache[track.videoId] = recs
            _recommendations.value = recs
            _isLoadingRecommendations.value = false
            preloadNextTrackFromList(recs, track.videoId)
        }
    }

    private fun preloadNextTrackFromList(list: List<Track>, currentId: String) {
        val candidate = list.firstOrNull { it.videoId != currentId && it.videoId !in recentlyAutoplayedIds }
            ?: list.firstOrNull { it.videoId != currentId }
        if (candidate != null) {
            preloadNextTrack(candidate)
        }
    }

    private fun preloadNextTrack(nextTrack: Track) {
        viewModelScope.launch {
            try {
                // 1. Preload thumbnail into Coil cache
                val thumbUrl = ThumbnailUtils.resolveOptimizedUrl(nextTrack.thumbnailUrl, nextTrack.videoId)
                if (thumbUrl.isNotBlank()) {
                    val request = ImageRequest.Builder(getApplication())
                        .data(thumbUrl)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build()
                    Coil.imageLoader(getApplication()).enqueue(request)
                }
                // 2. Pre-resolve stream media metadata
                if (preloadedMedia?.track?.externalId != nextTrack.videoId) {
                    preloadedMedia = repository.resolveMedia(nextTrack, _playerState.value.preferredSourceMode)
                }
            } catch (_: Exception) {}
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        autoplayJob?.cancel()
        targetSeekSec = -1f
        lastSeekTimestamp = System.currentTimeMillis() + 400

        viewModelScope.launch {
            repository.recordPlayed(track)
            val isFav = repository.isFavorite(track.videoId)
            val updatedTrack = track.copy(isFavorite = isFav)

            // Use preloaded media if already resolved for this track
            val resolved = if (preloadedMedia != null && preloadedMedia?.track?.externalId == track.videoId) {
                val media = preloadedMedia!!
                preloadedMedia = null
                media
            } else {
                repository.resolveMedia(updatedTrack, _playerState.value.preferredSourceMode)
            }

            val initialDuration = parseDurationSeconds(updatedTrack.duration)

            _playerState.update { current ->
                val q = newQueue ?: if (current.queue.any { it.videoId == track.videoId }) current.queue else listOf(track) + current.queue
                val idx = q.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)
                current.copy(
                    currentTrack = updatedTrack,
                    resolvedMedia = resolved,
                    playbackType = resolved.playbackType,
                    isPlaying = true,
                    isBuffering = true,
                    queue = q,
                    queueIndex = idx,
                    currentPositionSec = 0f,
                    totalDurationSec = if (initialDuration > 0f) initialDuration else 0f,
                    seekTargetSec = 0f
                )
            }

            // Fetch contextual recommendations asynchronously
            loadRecommendations(updatedTrack)

            // Direct audio playback vs YouTube IFrame playback
            if (resolved.playbackType == PlaybackType.DIRECT_AUDIO && !resolved.streamUrl.isNullOrBlank()) {
                audioPlaybackManager.playDirectStream(
                    url = resolved.streamUrl,
                    onCompletion = { onTrackFinished() },
                    onError = { onPlaybackError(it) }
                )
            } else {
                audioPlaybackManager.stop()
            }

            showMessage("NOW PLAYING: [${resolved.sourceName.uppercase()}] ${updatedTrack.title.take(24)}")
        }
    }

    fun togglePlayPause() {
        val current = _playerState.value
        if (current.currentTrack == null) return

        if (current.playbackType == PlaybackType.DIRECT_AUDIO) {
            if (current.isPlaying) {
                audioPlaybackManager.pause()
            } else {
                audioPlaybackManager.resume()
            }
        } else {
            _playerState.update { it.copy(isPlaying = !it.isPlaying) }
        }
    }

    fun setPlayingState(isPlaying: Boolean) {
        if (isPlaying) {
            consecutiveFailures = 0
        }
        _playerState.update { it.copy(isPlaying = isPlaying, isBuffering = false) }
    }

    fun setBufferingState(isBuffering: Boolean) {
        _playerState.update { it.copy(isBuffering = isBuffering) }
    }

    fun toggleVideoMode() {
        _playerState.update { it.copy(isVideoMode = !it.isVideoMode) }
    }

    fun nextTrack() {
        advanceToNextTrackOrAutoplay()
    }

    fun previousTrack() {
        val current = _playerState.value
        if (current.queue.isEmpty()) return

        val prevIndex = if (current.queueIndex > 0) current.queueIndex - 1 else current.queue.lastIndex
        val prev = current.queue[prevIndex]
        playTrack(prev)
    }

    fun toggleLoop() {
        _playerState.update { it.copy(isLooping = !it.isLooping) }
        showMessage(if (_playerState.value.isLooping) "LOOP: ACTIVE" else "LOOP: OFF")
    }

    fun toggleShuffle() {
        _playerState.update { it.copy(isShuffling = !it.isShuffling) }
        showMessage(if (_playerState.value.isShuffling) "SHUFFLE: ACTIVE" else "SHUFFLE: OFF")
    }

    fun seekTo(seconds: Float) {
        lastSeekTimestamp = System.currentTimeMillis()
        targetSeekSec = seconds
        val current = _playerState.value
        if (current.playbackType == PlaybackType.DIRECT_AUDIO) {
            audioPlaybackManager.seekTo(seconds)
        }
        _playerState.update {
            it.copy(
                currentPositionSec = seconds,
                seekTargetSec = seconds,
                isBuffering = false
            )
        }
    }

    fun clearSeekTarget() {
        _playerState.update { it.copy(seekTargetSec = null) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 100f)
        audioPlaybackManager.setVolume(clamped)
        _playerState.update { it.copy(volume = clamped, isMuted = clamped <= 0f) }
    }

    fun toggleMute() {
        _playerState.update {
            val newMuted = !it.isMuted
            val vol = if (newMuted) 0f else 100f
            audioPlaybackManager.setVolume(vol)
            it.copy(isMuted = newMuted, volume = vol)
        }
    }

    fun togglePlayerExpanded() {
        _playerState.update { it.copy(isPlayerExpanded = !it.isPlayerExpanded) }
    }

    fun toggleQueueVisibility() {
        _playerState.update { it.copy(isQueueVisible = !it.isQueueVisible) }
    }

    fun addToQueue(track: Track) {
        _playerState.update { current ->
            current.copy(queue = current.queue + track)
        }
        showMessage("QUEUED: ${track.title.take(15)}")
    }

    fun removeFromQueue(index: Int) {
        _playerState.update { current ->
            val mutable = current.queue.toMutableList()
            if (index in mutable.indices) {
                mutable.removeAt(index)
            }
            current.copy(queue = mutable)
        }
    }

    fun clearQueue() {
        val current = _playerState.value.currentTrack
        _playerState.update {
            it.copy(queue = if (current != null) listOf(current) else emptyList(), queueIndex = 0)
        }
        showMessage("QUEUE CLEARED")
    }

    fun onPlayerProgress(currentSec: Float, durationSec: Float) {
        val now = System.currentTimeMillis()
        if (now - lastSeekTimestamp < 800) {
            if (targetSeekSec >= 0f && kotlin.math.abs(currentSec - targetSeekSec) > 2f) {
                return
            }
        }
        targetSeekSec = -1f

        if (_playerState.value.playbackType == PlaybackType.YOUTUBE_EMBED) {
            _playerState.update { current ->
                if (!current.isPlaying) return@update current // Freeze position when paused
                val prev = current.currentPositionSec
                // Prevent jitter: monotonic advancement forward unless scrubbed or looped
                val newPos = if (currentSec >= prev || (prev - currentSec > 4f)) {
                    currentSec
                } else {
                    prev
                }
                current.copy(
                    currentPositionSec = newPos,
                    totalDurationSec = if (durationSec > 0) durationSec else current.totalDurationSec,
                    isBuffering = false
                )
            }
        }

        // Proactive Radio buffer replenishment: request more tracks when queue is running low
        if (_radioState.value.isRadioActive && !_radioState.value.isLoading && _radioState.value.error == null) {
            val remaining = _playerState.value.queue.size - 1 - _playerState.value.queueIndex
            if (remaining <= 2 && currentSec > 8f) {
                loadMoreRadioTracks()
            }
        }
    }

    fun onTrackFinished() {
        val current = _playerState.value
        val track = current.currentTrack ?: return

        // 1. Loop check
        if (current.isLooping) {
            seekTo(0f)
            _playerState.update { it.copy(isPlaying = true) }
            if (current.playbackType == PlaybackType.DIRECT_AUDIO) {
                current.resolvedMedia?.streamUrl?.let { url ->
                    audioPlaybackManager.playDirectStream(
                        url = url,
                        onCompletion = { onTrackFinished() },
                        onError = { onPlaybackError(it) }
                    )
                }
            }
            return
        }

        // 2. Autoplay setting check (Radio overrides and continues unless user stopped radio)
        if (!current.isAutoplayEnabled && !_radioState.value.isRadioActive) {
            _playerState.update { it.copy(isPlaying = false, currentPositionSec = 0f) }
            audioPlaybackManager.stop()
            showMessage("AUTOPLAY OFF: PLAYBACK STOPPED")
            return
        }

        // 3. Prevent duplicate completion triggers for the same song within 1.5 seconds
        val now = System.currentTimeMillis()
        if (track.videoId == lastCompletedTrackId && now - lastCompletedTimestamp < 1500L) {
            return
        }
        lastCompletedTrackId = track.videoId
        lastCompletedTimestamp = now

        // 4. Trigger intelligent autoplay transition
        advanceToNextTrackOrAutoplay()
    }

    fun advanceToNextTrackOrAutoplay() {
        val current = _playerState.value
        val currentTrack = current.currentTrack ?: return
        if (isAdvancingAutoplay) return
        isAdvancingAutoplay = true

        autoplayJob?.cancel()
        autoplayJob = viewModelScope.launch {
            try {
                // Step A: Check if the queue contains upcoming tracks
                val nextQueueIndex = current.queueIndex + 1
                if (nextQueueIndex in current.queue.indices) {
                    val nextTrack = if (current.isShuffling) {
                        val remaining = current.queue.drop(nextQueueIndex)
                        remaining.random()
                    } else {
                        current.queue[nextQueueIndex]
                    }

                    // Replenish radio if queue is nearly finished
                    if (_radioState.value.isRadioActive && (current.queue.size - nextQueueIndex <= 2)) {
                        loadMoreRadioTracks()
                    }

                    playTrack(nextTrack)
                    return@launch
                }

                // Step A.2: If Radio is active and queue ended, immediately fetch more related radio tracks
                if (_radioState.value.isRadioActive) {
                    _radioState.update { it.copy(isLoading = true, error = null) }
                    val seed = _radioState.value.seedTrack ?: currentTrack
                    val excluded = _radioState.value.radioHistory.toSet() + currentTrack.videoId
                    val more = repository.fetchRadioTracks(seed, _playerState.value.preferredSourceMode, excluded)
                    if (more.isNotEmpty()) {
                        val nextTrack = more.first()
                        val newHistory = _radioState.value.radioHistory + more.map { it.videoId }
                        _radioState.update { it.copy(isLoading = false, radioHistory = newHistory) }
                        playTrack(nextTrack, current.queue + more)
                        return@launch
                    } else {
                        _radioState.update { it.copy(isLoading = false, error = "Couldn't find more tracks.") }
                    }
                }

                // Step B: Queue is finished/single track. Pull from loaded recommendations
                val unplayedRecs = _recommendations.value.filter {
                    it.videoId != currentTrack.videoId && it.videoId !in recentlyAutoplayedIds
                }
                if (unplayedRecs.isNotEmpty()) {
                    val chosen = unplayedRecs.first()
                    recentlyAutoplayedIds.add(chosen.videoId)
                    if (recentlyAutoplayedIds.size > 50) {
                        recentlyAutoplayedIds.remove(recentlyAutoplayedIds.first())
                    }
                    val updatedQueue = current.queue + unplayedRecs
                    playTrack(chosen, updatedQueue)
                    return@launch
                }

                // Step C: Recommendations empty or exhausted. Fetch fresh related tracks
                _playerState.update { it.copy(isBuffering = true) }
                val freshRecs = repository.fetchRecommendedTracks(currentTrack, _playerState.value.preferredSourceMode)
                val validCandidate = freshRecs.firstOrNull {
                    it.videoId != currentTrack.videoId && it.videoId !in recentlyAutoplayedIds
                } ?: freshRecs.firstOrNull { it.videoId != currentTrack.videoId }

                if (validCandidate != null) {
                    recentlyAutoplayedIds.add(validCandidate.videoId)
                    if (recentlyAutoplayedIds.size > 50) {
                        recentlyAutoplayedIds.remove(recentlyAutoplayedIds.first())
                    }
                    val updatedQueue = current.queue + freshRecs
                    playTrack(validCandidate, updatedQueue)
                    return@launch
                }

                // Step D: Ultimate fallback: Trending tracks
                val trending = repository.fetchTrendingTracks(_playerState.value.preferredSourceMode)
                val trendCandidate = trending.firstOrNull { it.videoId != currentTrack.videoId }
                if (trendCandidate != null) {
                    playTrack(trendCandidate, current.queue + trending)
                } else {
                    _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
                    showMessage("AUTOPLAY QUEUE COMPLETED")
                }
            } finally {
                isAdvancingAutoplay = false
            }
        }
    }

    fun onPlaybackError(errorCode: Int) {
        val current = _playerState.value
        consecutiveFailures++
        if (current.isAutoplayEnabled && consecutiveFailures <= 3) {
            showMessage("TRACK UNAVAILABLE, AUTOPLAYING NEXT...")
            viewModelScope.launch {
                delay(350)
                advanceToNextTrackOrAutoplay()
            }
        } else {
            _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
            showMessage(if (consecutiveFailures > 3) "MULTIPLE PLAYBACK ERRORS - CHECK NETWORK" else "PLAYBACK ERROR (CODE $errorCode)")
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val isFav = repository.toggleFavorite(track)
            if (_playerState.value.currentTrack?.videoId == track.videoId) {
                _playerState.update { it.copy(currentTrack = it.currentTrack?.copy(isFavorite = isFav)) }
            }
            showMessage(if (isFav) "SAVED TO FAVORITES" else "REMOVED FROM FAVORITES")
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        suggestionJob?.cancel()
        if (query.isNotBlank()) {
            suggestionJob = viewModelScope.launch {
                delay(250)
                val suggestions = YouTubeSearchService.getSearchSuggestions(query)
                _uiState.update { it.copy(searchSuggestions = suggestions) }
            }
        } else {
            _uiState.update { it.copy(searchSuggestions = emptyList(), searchResults = emptyList(), isSearching = false) }
        }
    }

    fun performSearch(queryOverride: String? = null) {
        val query = (queryOverride ?: _uiState.value.searchQuery).trim()
        if (query.isBlank()) return

        _uiState.update { it.copy(searchQuery = query, isSearching = true, searchSuggestions = emptyList()) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val results = repository.search(query, _uiState.value.preferredSourceMode)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
            if (results.isEmpty()) {
                showMessage("NO RESULTS FOR: $query")
            } else {
                showMessage("FOUND ${results.size} MULTI-SOURCE TRACKS")
            }
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false, searchSuggestions = emptyList()) }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name.trim().uppercase())
            showMessage("PLAYLIST CREATED: ${name.trim().uppercase()}")
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            _uiState.update { it.copy(selectedPlaylist = null, selectedPlaylistTracks = emptyList()) }
            showMessage("PLAYLIST DELETED")
        }
    }

    fun openPlaylist(playlist: PlaylistEntity) {
        _uiState.update { it.copy(selectedPlaylist = playlist) }
        viewModelScope.launch {
            repository.getTracksForPlaylist(playlist.id).collectLatest { tracks ->
                _uiState.update { it.copy(selectedPlaylistTracks = tracks) }
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
            showMessage("ADDED TO PLAYLIST")
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, videoId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, videoId)
            showMessage("REMOVED FROM PLAYLIST")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            showMessage("HISTORY CLEARED")
        }
    }

    fun startRadio(seedTrack: Track) {
        radioFetchJob?.cancel()
        _radioState.value = RadioState(
            isRadioActive = true,
            seedTrack = seedTrack,
            isLoading = true,
            error = null,
            radioHistory = listOf(seedTrack.videoId)
        )
        _playerState.update { it.copy(isRadioActive = true, radioSeedTrack = seedTrack) }
        showMessage("RADIO STARTED: ${seedTrack.title.take(20)}")

        val currentPlaying = _playerState.value.currentTrack
        if (currentPlaying?.videoId != seedTrack.videoId) {
            playTrack(seedTrack, listOf(seedTrack))
        } else {
            _playerState.update { current ->
                current.copy(queue = listOf(seedTrack), queueIndex = 0)
            }
        }

        radioFetchJob = viewModelScope.launch {
            try {
                val excluded = _radioState.value.radioHistory.toSet() + seedTrack.videoId
                val related = repository.fetchRadioTracks(seedTrack, _playerState.value.preferredSourceMode, excluded)
                if (related.isNotEmpty()) {
                    val newHistory = _radioState.value.radioHistory + related.map { it.videoId }
                    _radioState.update {
                        it.copy(isLoading = false, error = null, radioHistory = newHistory)
                    }
                    _playerState.update { current ->
                        val existingIds = current.queue.map { it.videoId }.toSet()
                        val uniqueNew = related.filter { it.videoId !in existingIds }
                        current.copy(queue = current.queue + uniqueNew)
                    }
                    preloadNextTrackFromList(related, seedTrack.videoId)
                } else {
                    _radioState.update {
                        it.copy(isLoading = false, error = "Couldn't find more tracks.")
                    }
                }
            } catch (e: Exception) {
                _radioState.update {
                    it.copy(isLoading = false, error = "Couldn't find more tracks.")
                }
            }
        }
    }

    fun loadMoreRadioTracks(seed: Track? = null) {
        if (_radioState.value.isLoading) return
        val seedTrack = seed ?: _radioState.value.seedTrack ?: _playerState.value.currentTrack ?: return

        _radioState.update { it.copy(isLoading = true, error = null) }
        radioFetchJob?.cancel()
        radioFetchJob = viewModelScope.launch {
            try {
                val currentPlayingId = _playerState.value.currentTrack?.videoId ?: ""
                val excluded = _radioState.value.radioHistory.toSet() + currentPlayingId + seedTrack.videoId
                val more = repository.fetchRadioTracks(seedTrack, _playerState.value.preferredSourceMode, excluded)
                if (more.isNotEmpty()) {
                    val newHistory = _radioState.value.radioHistory + more.map { it.videoId }
                    _radioState.update {
                        it.copy(isLoading = false, error = null, radioHistory = newHistory)
                    }
                    _playerState.update { current ->
                        val existingIds = current.queue.map { it.videoId }.toSet()
                        val unique = more.filter { it.videoId !in existingIds }
                        current.copy(queue = current.queue + unique)
                    }
                } else {
                    _radioState.update {
                        it.copy(isLoading = false, error = "Couldn't find more tracks.")
                    }
                }
            } catch (e: Exception) {
                _radioState.update {
                    it.copy(isLoading = false, error = "Couldn't find more tracks.")
                }
            }
        }
    }

    fun retryRadioFetch() {
        loadMoreRadioTracks()
    }

    fun stopRadio() {
        radioFetchJob?.cancel()
        _radioState.value = RadioState()
        _playerState.update { it.copy(isRadioActive = false, radioSeedTrack = null) }
        showMessage("RADIO STOPPED")
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(statusMessage = msg) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlaybackManager.stop()
    }
}

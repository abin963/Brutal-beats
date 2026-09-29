package com.example.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class PreferredSourceMode {
    AUTO,        // Standard direct YouTube embed, JioSaavn for explicit 320k tracks
    YOUTUBE,     // Always uses YouTube IFrame stream
    JIOSAAVN     // Prefers JioSaavn 320kbps direct stream
}

object SourceResolver {

    suspend fun resolve(
        track: UnifiedTrack,
        preferredMode: PreferredSourceMode = PreferredSourceMode.AUTO
    ): ResolvedMedia = withContext(Dispatchers.IO) {
        val ytSource = SourceRegistry.getYouTubeSource()
        val saavnSource = SourceRegistry.getJioSaavnSource()

        when (preferredMode) {
            PreferredSourceMode.YOUTUBE -> {
                // Force YouTube resolution
                if (track.sourceId == "YOUTUBE") {
                    return@withContext ResolvedMedia(
                        track = track,
                        playbackType = PlaybackType.YOUTUBE_EMBED,
                        streamUrl = null,
                        youtubeVideoId = track.externalId,
                        sourceName = "YouTube",
                        resolutionNote = "DIRECT YOUTUBE"
                    )
                }
                // Match to YouTube
                val ytCandidates = ytSource.search("${track.title} ${track.artist}")
                val match = TrackMatcher.findBestMatch(track.title, track.artist, track.durationSec, ytCandidates)
                val targetTrack = match?.matchedTrack ?: ytCandidates.firstOrNull() ?: track
                ResolvedMedia(
                    track = targetTrack,
                    playbackType = PlaybackType.YOUTUBE_EMBED,
                    streamUrl = null,
                    youtubeVideoId = targetTrack.externalId,
                    sourceName = "YouTube",
                    resolutionNote = if (match != null) "MATCHED YT (${(match.confidence * 100).toInt()}%)" else "FALLBACK YT"
                )
            }

            PreferredSourceMode.JIOSAAVN -> {
                // Force JioSaavn resolution
                val saavnMedia = saavnSource.resolveStream(track)
                if (saavnMedia != null && !saavnMedia.streamUrl.isNullOrBlank()) {
                    return@withContext saavnMedia
                }
                // Fallback to YouTube if JioSaavn failed
                ytSource.resolveStream(track) ?: fallbackMedia(track)
            }

            PreferredSourceMode.AUTO -> {
                // 1. If track already has direct stream url (e.g. JioSaavn source)
                if (!track.directStreamUrl.isNullOrBlank() && track.sourceId == "JIOSAAVN") {
                    return@withContext ResolvedMedia(
                        track = track,
                        playbackType = PlaybackType.DIRECT_AUDIO,
                        streamUrl = track.directStreamUrl,
                        youtubeVideoId = null,
                        sourceName = "JioSaavn",
                        resolutionNote = "DIRECT STREAM (320K)"
                    )
                }

                // 2. Direct YouTube playback
                if (track.sourceId == "YOUTUBE" || track.externalId.isNotBlank()) {
                    return@withContext ResolvedMedia(
                        track = track,
                        playbackType = PlaybackType.YOUTUBE_EMBED,
                        streamUrl = null,
                        youtubeVideoId = track.externalId,
                        sourceName = "YouTube",
                        resolutionNote = "YOUTUBE STREAM"
                    )
                }

                // 3. Try resolving via JioSaavn
                val saavnMedia = saavnSource.resolveStream(track)
                if (saavnMedia != null && !saavnMedia.streamUrl.isNullOrBlank()) {
                    return@withContext saavnMedia
                }

                // 4. Fallback to YouTube
                ytSource.resolveStream(track) ?: fallbackMedia(track)
            }
        }
    }

    private fun fallbackMedia(track: UnifiedTrack): ResolvedMedia {
        return ResolvedMedia(
            track = track,
            playbackType = if (!track.directStreamUrl.isNullOrBlank()) PlaybackType.DIRECT_AUDIO else PlaybackType.YOUTUBE_EMBED,
            streamUrl = track.directStreamUrl,
            youtubeVideoId = if (track.sourceId == "YOUTUBE") track.externalId else "gAjR4_CbPpQ",
            sourceName = track.sourceId,
            resolutionNote = "STANDARD FALLBACK"
        )
    }
}

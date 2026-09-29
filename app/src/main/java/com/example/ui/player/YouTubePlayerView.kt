package com.example.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.BrutalDeepBlack

class YouTubeBridge(
    private val onStateChanged: (Boolean) -> Unit,
    private val onBufferingChanged: (Boolean) -> Unit = {},
    private val onTimeProgress: (Float, Float) -> Unit,
    private val onTrackEnded: () -> Unit,
    private val onErrorOccurred: (Int) -> Unit,
    private val onNext: () -> Unit = {},
    private val onPrevious: () -> Unit = {}
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onReady() {
        mainHandler.post {
            onStateChanged(true)
            onBufferingChanged(false)
        }
    }

    @JavascriptInterface
    fun onStateChange(state: Int) {
        mainHandler.post {
            when (state) {
                1 -> { // PLAYING
                    onStateChanged(true)
                    onBufferingChanged(false)
                }
                2 -> { // PAUSED
                    onStateChanged(false)
                    onBufferingChanged(false)
                }
                3 -> { // BUFFERING
                    onBufferingChanged(true)
                }
                0 -> { // ENDED
                    onStateChanged(false)
                    onBufferingChanged(false)
                    onTrackEnded()
                }
            }
        }
    }

    @JavascriptInterface
    fun onProgress(current: Float, duration: Float) {
        mainHandler.post {
            onTimeProgress(current, duration)
        }
    }

    @JavascriptInterface
    fun onError(code: Int) {
        mainHandler.post {
            onBufferingChanged(false)
            onErrorOccurred(code)
        }
    }

    @JavascriptInterface
    fun onNextTrack() {
        mainHandler.post {
            onNext()
        }
    }

    @JavascriptInterface
    fun onPrevTrack() {
        mainHandler.post {
            onPrevious()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerView(
    videoId: String,
    isPlaying: Boolean,
    volume: Float,
    seekToSeconds: Float?,
    onStateChanged: (Boolean) -> Unit,
    onBufferingChanged: (Boolean) -> Unit = {},
    onTimeProgress: (Float, Float) -> Unit,
    onTrackEnded: () -> Unit,
    onError: (Int) -> Unit,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    trackTitle: String = "",
    trackArtist: String = "",
    artworkUrl: String = "",
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentLoadedVideoId by remember { mutableStateOf<String?>(null) }

    // Synchronize play/pause state
    LaunchedEffect(isPlaying) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (isPlaying) {
            wv.evaluateJavascript("resumeVideo();", null)
        } else {
            wv.evaluateJavascript("pauseVideo();", null)
        }
    }

    // Synchronize volume
    LaunchedEffect(volume) {
        val wv = webViewRef ?: return@LaunchedEffect
        wv.evaluateJavascript("setVolume(${volume.toInt()});", null)
    }

    // Synchronize seek
    LaunchedEffect(seekToSeconds) {
        val sec = seekToSeconds ?: return@LaunchedEffect
        val wv = webViewRef ?: return@LaunchedEffect
        wv.evaluateJavascript("seekTo($sec);", null)
    }

    // Load new track when videoId changes
    LaunchedEffect(videoId) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (currentLoadedVideoId != null && currentLoadedVideoId != videoId) {
            val safeTitle = trackTitle.replace("'", "\\'")
            val safeArtist = trackArtist.replace("'", "\\'")
            val safeArt = artworkUrl.replace("'", "\\'")
            wv.evaluateJavascript("playTrack('$videoId', '$safeTitle', '$safeArtist', '$safeArt');", null)
            currentLoadedVideoId = videoId
        }
    }

    // Update mediaSession metadata on track details update
    LaunchedEffect(trackTitle, trackArtist, artworkUrl) {
        val wv = webViewRef ?: return@LaunchedEffect
        val safeTitle = trackTitle.replace("'", "\\'")
        val safeArtist = trackArtist.replace("'", "\\'")
        val safeArt = artworkUrl.replace("'", "\\'")
        wv.evaluateJavascript("updateMediaSessionMetadata('$safeTitle', '$safeArtist', '$safeArt');", null)
    }

    Box(modifier = modifier.background(BrutalDeepBlack)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                createYouTubeWebView(
                    context = context,
                    initialVideoId = videoId,
                    initialTitle = trackTitle,
                    initialArtist = trackArtist,
                    initialArtwork = artworkUrl,
                    bridge = YouTubeBridge(
                        onStateChanged = onStateChanged,
                        onBufferingChanged = onBufferingChanged,
                        onTimeProgress = onTimeProgress,
                        onTrackEnded = onTrackEnded,
                        onErrorOccurred = onError,
                        onNext = onNext,
                        onPrevious = onPrevious
                    )
                ).also {
                    webViewRef = it
                    currentLoadedVideoId = videoId
                }
            },
            update = { wv ->
                if (currentLoadedVideoId != videoId) {
                    val safeTitle = trackTitle.replace("'", "\\'")
                    val safeArtist = trackArtist.replace("'", "\\'")
                    val safeArt = artworkUrl.replace("'", "\\'")
                    wv.evaluateJavascript("playTrack('$videoId', '$safeTitle', '$safeArtist', '$safeArt');", null)
                    currentLoadedVideoId = videoId
                }
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createYouTubeWebView(
    context: Context,
    initialVideoId: String,
    initialTitle: String = "",
    initialArtist: String = "",
    initialArtwork: String = "",
    bridge: YouTubeBridge
): WebView {
    return WebView(context).apply {
        setBackgroundColor(AndroidColor.BLACK)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = false
            allowContentAccess = false
        }
        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                try {
                    view?.destroy()
                } catch (_: Exception) {}
                return true // Prevent Android OS from killing the host app process
            }
        }
        addJavascriptInterface(bridge, "AndroidBridge")

        val html = buildYouTubeHtml(initialVideoId, initialTitle, initialArtist, initialArtwork)
        loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
    }
}

private fun buildYouTubeHtml(
    videoId: String,
    title: String = "",
    artist: String = "",
    artwork: String = ""
): String = """
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <style>
    * { box-sizing: border-box; }
    html, body {
      margin: 0;
      padding: 0;
      width: 100%;
      height: 100%;
      background-color: #000000;
      overflow: hidden;
    }
    #player-container {
      width: 100%;
      height: 100%;
      position: absolute;
      top: 0;
      left: 0;
    }
    #player {
      width: 100%;
      height: 100%;
    }
  </style>
</head>
<body>
  <div id="player-container">
    <div id="player"></div>
  </div>
  <script src="https://www.youtube.com/iframe_api"></script>
  <script>
    var player;
    var progressInterval = null;
    var currentVid = '$videoId';
    var pendingVid = null;
    var currentTitle = '${title.replace("'", "\\'")}';
    var currentArtist = '${artist.replace("'", "\\'")}';
    var currentArtwork = '${artwork.replace("'", "\\'")}';

    function initMediaSession() {
      if ('mediaSession' in navigator) {
        updateMediaSessionMetadata(currentTitle, currentArtist, currentArtwork);
        try {
          navigator.mediaSession.setActionHandler('play', function() { resumeVideo(); });
          navigator.mediaSession.setActionHandler('pause', function() { pauseVideo(); });
          navigator.mediaSession.setActionHandler('nexttrack', function() {
            if (window.AndroidBridge && window.AndroidBridge.onNextTrack) {
              window.AndroidBridge.onNextTrack();
            }
          });
          navigator.mediaSession.setActionHandler('previoustrack', function() {
            if (window.AndroidBridge && window.AndroidBridge.onPrevTrack) {
              window.AndroidBridge.onPrevTrack();
            }
          });
          navigator.mediaSession.setActionHandler('seekto', function(details) {
            if (details.seekTime !== undefined) {
              seekTo(details.seekTime);
            }
          });
        } catch (e) {}
      }
    }

    function updateMediaSessionMetadata(title, artist, artworkUrl) {
      currentTitle = title || currentTitle;
      currentArtist = artist || currentArtist;
      currentArtwork = artworkUrl || currentArtwork;
      if ('mediaSession' in navigator && window.MediaMetadata) {
        try {
          var artworkList = [];
          if (currentArtwork) {
            artworkList.push({ src: currentArtwork, sizes: '512x512', type: 'image/jpeg' });
          }
          navigator.mediaSession.metadata = new MediaMetadata({
            title: currentTitle || 'NYX Music',
            artist: currentArtist || 'High Quality Audio',
            album: 'NYX Music Player',
            artwork: artworkList
          });
        } catch (e) {}
      }
    }

    function onYouTubeIframeAPIReady() {
      var initialVid = pendingVid || currentVid;
      player = new YT.Player('player', {
        videoId: initialVid,
        playerVars: {
          'autoplay': 1,
          'playsinline': 1,
          'controls': 1,
          'rel': 0,
          'modestbranding': 1,
          'fs': 1,
          'enablejsapi': 1,
          'iv_load_policy': 3,
          'origin': 'https://www.youtube-nocookie.com'
        },
        events: {
          'onReady': onPlayerReady,
          'onStateChange': onPlayerStateChange,
          'onError': onPlayerError
        }
      });
      pendingVid = null;
      initMediaSession();
    }

    function startProgressLoop() {
      if (progressInterval) return;
      progressInterval = setInterval(function() {
        try {
          if (player && typeof player.getPlayerState === 'function') {
            var state = player.getPlayerState();
            // ONLY report progress when actively PLAYING (state === 1)
            if (state === 1 && typeof player.getCurrentTime === 'function' && typeof player.getDuration === 'function') {
              var cur = player.getCurrentTime();
              var dur = player.getDuration();
              if (window.AndroidBridge && dur > 0) {
                window.AndroidBridge.onProgress(cur, dur);
              }
              if ('mediaSession' in navigator && typeof navigator.mediaSession.setPositionState === 'function') {
                try {
                  navigator.mediaSession.setPositionState({
                    duration: dur,
                    playbackRate: 1,
                    position: cur
                  });
                } catch(e) {}
              }
            }
          }
        } catch(e) {}
      }, 250);
    }

    function stopProgressLoop() {
      if (progressInterval) {
        clearInterval(progressInterval);
        progressInterval = null;
      }
    }

    function onPlayerReady(event) {
      if (window.AndroidBridge) {
        window.AndroidBridge.onReady();
      }
      try {
        if (pendingVid) {
          event.target.loadVideoById(pendingVid);
          pendingVid = null;
        } else {
          event.target.playVideo();
        }
      } catch(e) {}
      initMediaSession();
    }

    function onPlayerStateChange(event) {
      var state = event.data;
      if (state === 1) {
        startProgressLoop();
        if ('mediaSession' in navigator) {
          navigator.mediaSession.playbackState = 'playing';
        }
      } else if (state === 2) {
        stopProgressLoop();
        if ('mediaSession' in navigator) {
          navigator.mediaSession.playbackState = 'paused';
        }
      } else {
        stopProgressLoop();
        if ('mediaSession' in navigator) {
          navigator.mediaSession.playbackState = 'none';
        }
      }
      if (window.AndroidBridge) {
        window.AndroidBridge.onStateChange(state);
      }
    }

    function onPlayerError(event) {
      stopProgressLoop();
      if (window.AndroidBridge) {
        window.AndroidBridge.onError(event.data);
      }
    }

    function playTrack(vid, title, artist, artworkUrl) {
      currentVid = vid;
      if (title || artist || artworkUrl) {
        updateMediaSessionMetadata(title, artist, artworkUrl);
      }
      try {
        if (player && typeof player.loadVideoById === 'function') {
          player.loadVideoById(vid);
        } else {
          pendingVid = vid;
        }
      } catch(e) {
        pendingVid = vid;
      }
    }

    function pauseVideo() {
      stopProgressLoop();
      if ('mediaSession' in navigator) {
        navigator.mediaSession.playbackState = 'paused';
      }
      try {
        if (player && player.pauseVideo) {
          player.pauseVideo();
        }
      } catch(e) {}
    }

    function resumeVideo() {
      if ('mediaSession' in navigator) {
        navigator.mediaSession.playbackState = 'playing';
      }
      try {
        if (player && player.playVideo) {
          player.playVideo();
        }
      } catch(e) {}
    }

    function seekTo(sec) {
      try {
        if (player && player.seekTo) {
          player.seekTo(sec, true);
        }
      } catch(e) {}
    }

    function setVolume(vol) {
      try {
        if (player && player.setVolume) {
          player.setVolume(vol);
        }
      } catch(e) {}
    }
  </script>
</body>
</html>
""".trimIndent()


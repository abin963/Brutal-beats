package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.NeonPink

object ThumbnailUtils {
    /**
     * Resolves the most reliable high-resolution thumbnail URL.
     */
    fun resolveOptimizedUrl(rawUrl: String?, videoId: String? = null): String {
        val cleanVid = videoId?.removePrefix("yt:")?.removePrefix("saavn:")?.removePrefix("js:")?.trim()

        if (!rawUrl.isNullOrBlank()) {
            var url = rawUrl.trim()
                .replace("&amp;", "&")
                .replace("\\\"", "")
                .replace("\"", "")

            if (url.startsWith("http://")) {
                url = url.replace("http://", "https://")
            }
            // Upgrade JioSaavn thumbnail resolution to 500x500 for crisp artwork
            if (url.contains("jiosaavn") || url.contains("saavn")) {
                url = url.replace("50x50", "500x500").replace("150x150", "500x500")
            }
            // Ensure proper YouTube CDN domain
            if (url.contains("img.youtube.com")) {
                url = url.replace("img.youtube.com", "i.ytimg.com")
            }
            if (url.startsWith("https://") || url.startsWith("http://")) {
                return url
            }
        }

        // Fallback to YouTube CDN if cleanVid is a standard 11-char videoId
        if (!cleanVid.isNullOrBlank() && cleanVid.length == 11) {
            return "https://i.ytimg.com/vi/$cleanVid/hqdefault.jpg"
        }

        return ""
    }

    /**
     * Fallback URL if primary high-res fails (e.g., if 500x500 is 404 on JioSaavn or hqdefault on YouTube).
     */
    fun resolveFallbackUrl(rawUrl: String?, videoId: String? = null): String {
        val cleanVid = videoId?.removePrefix("yt:")?.removePrefix("saavn:")?.removePrefix("js:")?.trim()

        if (!rawUrl.isNullOrBlank()) {
            var url = rawUrl.trim()
                .replace("&amp;", "&")
                .replace("\\\"", "")
                .replace("\"", "")

            if (url.startsWith("http://")) {
                url = url.replace("http://", "https://")
            }
            // 150x150 is always available on JioSaavn
            if (url.contains("jiosaavn") || url.contains("saavn")) {
                return url.replace("500x500", "150x150").replace("50x50", "150x150")
            }
            if (url.contains("img.youtube.com")) {
                return url.replace("img.youtube.com", "i.ytimg.com").replace("maxresdefault", "hqdefault")
            }
            return url
        }

        if (!cleanVid.isNullOrBlank() && cleanVid.length == 11) {
            return "https://i.ytimg.com/vi/$cleanVid/mqdefault.jpg"
        }

        return ""
    }
}

@Composable
fun BrutalThumbnail(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    videoId: String? = null,
    sourceId: String? = null,
    shape: Shape = RoundedCornerShape(8.dp),
    borderWidth: Dp = 1.dp,
    showSourceBadge: Boolean = true
) {
    val context = LocalContext.current
    var primaryFailed by remember(imageUrl, videoId) { mutableStateOf(false) }

    val activeUrl = remember(imageUrl, videoId, primaryFailed) {
        if (!primaryFailed) {
            val primary = ThumbnailUtils.resolveOptimizedUrl(imageUrl, videoId)
            if (primary.isNotBlank()) primary else ThumbnailUtils.resolveFallbackUrl(imageUrl, videoId)
        } else {
            ThumbnailUtils.resolveFallbackUrl(imageUrl, videoId)
        }
    }

    val imageRequest = remember(activeUrl) {
        ImageRequest.Builder(context)
            .data(activeUrl.ifBlank { null })
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .listener(
                onError = { _, _ ->
                    if (!primaryFailed) {
                        primaryFailed = true
                    }
                }
            )
            .build()
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), shape)
    ) {
        if (activeUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = NeonLime
                        )
                    }
                },
                error = {
                    ArtisticVinylFallback(
                        title = contentDescription ?: "BRUTAL BEATS",
                        sourceId = sourceId,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        } else {
            ArtisticVinylFallback(
                title = contentDescription ?: "BRUTAL BEATS",
                sourceId = sourceId,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Source badge overlay if requested
        if (showSourceBadge && !sourceId.isNullOrBlank()) {
            val isSaavn = sourceId.contains("SAAVN", ignoreCase = true)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(if (isSaavn) NeonCyan else Color.Black)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isSaavn) "SAAVN" else "YT",
                    color = if (isSaavn) Color.Black else Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ArtisticVinylFallback(
    title: String,
    sourceId: String?,
    modifier: Modifier = Modifier
) {
    val isSaavn = sourceId?.contains("SAAVN", ignoreCase = true) == true
    val accentColor = if (isSaavn) NeonCyan else NeonLime

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101014)),
        contentAlignment = Alignment.Center
    ) {
        // Subtle concentric vinyl grooves
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f
            for (i in 1..4) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f * i),
                    radius = maxR * (0.25f + i * 0.15f),
                    center = center,
                    style = Stroke(width = 1.2f)
                )
            }
        }

        // Center vinyl label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(1.5.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "BRUTAL BEATS",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}

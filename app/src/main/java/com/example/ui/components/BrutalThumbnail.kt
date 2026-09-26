package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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

object ThumbnailUtils {
    /**
     * Resolves the most reliable high-resolution thumbnail URL.
     */
    fun resolveOptimizedUrl(rawUrl: String?, videoId: String? = null): String {
        if (!rawUrl.isNullOrBlank()) {
            var url = rawUrl.trim()
            if (url.startsWith("http://")) {
                url = url.replace("http://", "https://")
            }
            // Upgrade JioSaavn thumbnail resolution
            if (url.contains("jiosaavn") || url.contains("saavn")) {
                url = url.replace("50x50", "500x500").replace("150x150", "500x500")
            }
            // Ensure proper YouTube CDN domain
            if (url.contains("img.youtube.com")) {
                url = url.replace("img.youtube.com", "i.ytimg.com")
            }
            return url
        }

        // Fallback to YouTube CDN if videoId is provided
        if (!videoId.isNullOrBlank() && videoId.length == 11) {
            return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
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
    val optimizedUrl = remember(imageUrl, videoId) {
        ThumbnailUtils.resolveOptimizedUrl(imageUrl, videoId)
    }

    val imageRequest = remember(optimizedUrl) {
        ImageRequest.Builder(context)
            .data(optimizedUrl.ifEmpty { null })
            .crossfade(200)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), shape)
    ) {
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
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            error = {
                // Highly styled music vinyl fallback card so it NEVER displays blank
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF141418)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Music",
                            tint = NeonLime,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "BEATS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }
        )

        // Source badge overlay if requested
        if (showSourceBadge && !sourceId.isNullOrBlank()) {
            val isSaavn = sourceId.contains("SAAVN", ignoreCase = true)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(if (isSaavn) NeonCyan else Color.Black)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = if (isSaavn) "SAAVN" else "YT",
                    color = if (isSaavn) Color.Black else Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

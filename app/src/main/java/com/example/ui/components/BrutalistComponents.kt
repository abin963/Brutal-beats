package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.ui.theme.*

/**
 * Premium Liquid Glass Surface with specular top reflection, translucent fill, and soft glow.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    backgroundColor: Color = GlassSurfaceDark,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    glowColor: Color = GlassGlow,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = tween(150),
        label = "glass_card_press"
    )

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            borderColor.copy(alpha = 0.55f),
            borderColor.copy(alpha = 0.15f),
            NyxPurple.copy(alpha = 0.25f)
        )
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true),
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = shape,
        color = backgroundColor,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderBrush)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NyxPurple.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = 400f,
                        center = Offset(0f, 0f)
                    )
                ),
            content = content
        )
    }
}

/**
 * Interactive Liquid Glass Button with smooth press animation and specular highlight.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NyxPurple,
    contentColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(24.dp),
    testTag: String = "liquid_glass_button",
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(150),
        label = "glass_btn_scale"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = if (enabled) backgroundColor else Color(0x33FFFFFF),
        modifier = modifier
            .scale(scale)
            .testTag(testTag)
            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.4f),
                    NyxPurpleLight.copy(alpha = 0.2f)
                )
            )
        ),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * Ambient artwork background with heavy blur, dark scrim, and violet gradient.
 * Smoothly crossfades when the song changes.
 */
@Composable
fun AmbientBackgroundLayer(
    artworkUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolvedUrl = remember(artworkUrl) {
        ThumbnailUtils.resolveOptimizedUrl(artworkUrl)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = resolvedUrl,
            animationSpec = tween(600),
            label = "ambient_bg_crossfade"
        ) { url ->
            if (url.isNotBlank()) {
                val imageRequest = remember(url) {
                    ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                }

                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                        .scale(1.25f)
                )
            }
        }

        // Dark gradient scrim + purple/violet ambient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBackground.copy(alpha = 0.88f),
                            Color(0xEE0B0818),
                            DarkBackground.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        // Radial purple atmospheric glow in upper-center
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NyxPurple.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        radius = 800f,
                        center = Offset(400f, 250f)
                    )
                )
        )
    }
}

/**
 * Legacy compatibility card mapped to Liquid Glass.
 */
@Composable
fun BrutalCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassSurfaceDark,
    borderColor: Color = GlassBorder,
    shadowColor: Color = Color.Black,
    borderWidth: Dp = 1.dp,
    shadowOffset: Dp = 0.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    LiquidGlassCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
        content = content
    )
}

/**
 * Legacy compatibility button mapped to Liquid Glass.
 */
@Composable
fun BrutalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NyxPurple,
    contentColor: Color = Color.White,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    shadowOffset: Dp = 0.dp,
    testTag: String = "brutal_button",
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    LiquidGlassButton(
        onClick = onClick,
        modifier = modifier,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        testTag = testTag,
        enabled = enabled,
        content = content
    )
}

/**
 * Liquid glass pill badge (e.g. NOW PLAYING, YT SOURCE, 320K).
 */
@Composable
fun BrutalBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NyxPurple.copy(alpha = 0.2f),
    textColor: Color = NyxPurpleLight,
    borderColor: Color = NyxPurple.copy(alpha = 0.6f),
    borderWidth: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp
        )
    }
}

/**
 * Liquid glass divider with subtle purple reflection.
 */
@Composable
fun BrutalDivider(
    modifier: Modifier = Modifier,
    color: Color = NyxSurfaceBorder,
    thickness: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        color,
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * Monospaced track number indicator.
 */
@Composable
fun BrutalTrackNumber(
    number: Int,
    modifier: Modifier = Modifier,
    color: Color = NyxPurpleLight
) {
    val formatted = if (number < 10) "#0$number" else "#$number"
    Text(
        text = formatted,
        modifier = modifier,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.5.sp
    )
}

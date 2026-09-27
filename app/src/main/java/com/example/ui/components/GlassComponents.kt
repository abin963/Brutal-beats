package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Ambient background container that adds subtle, performant blurred gradient blobs/orbs
 * behind the NYX music interface (Dark purple, violet, neon glow).
 */
@Composable
fun GlassAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark by ThemeManager.isDarkMode.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) DarkBackground else LightBackground)
            .drawBehind {
                if (isDark) {
                    // Deep violet-purple ambient orb (top-right)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AmbientGlowPurple,
                                AmbientGlowViolet.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.15f),
                            radius = size.width * 0.85f
                        )
                    )

                    // Subtle cyan-purple ambient glow orb (mid-left)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AmbientGlowViolet.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.12f, size.height * 0.58f),
                            radius = size.width * 0.75f
                        )
                    )

                    // Subtle deep magenta/pink neon accent glow (bottom-right)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AmbientGlowPink.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.8f, size.height * 0.88f),
                            radius = size.width * 0.6f
                        )
                    )
                } else {
                    // Light mode: soft lilac and lavender ambient tones
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x35EDE9FE),
                                Color(0x15DDD6FE),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.15f),
                            radius = size.width * 0.8f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x28F3E8FF),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.15f, size.height * 0.65f),
                            radius = size.width * 0.7f
                        )
                    )
                }
            },
        content = content
    )
}

/**
 * Premium Glassmorphism Card surface.
 * Features:
 * - 16-24dp rounded corners
 * - Semi-transparent gradient fill
 * - Directional specular border
 * - Soft purple/drop shadow
 * - Slightly brighter on press
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    isHighlighted: Boolean = false,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark by ThemeManager.isDarkMode.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(150),
        label = "glass_card_scale"
    )

    val baseModifier = modifier
        .scale(cardScale)
        .shadow(
            elevation = if (isHighlighted) 8.dp else 4.dp,
            shape = shape,
            ambientColor = if (isDark) NyxPurpleGlow else Color(0x1A000000),
            spotColor = if (isHighlighted) NyxPurple else Color(0x10000000)
        )
        .clip(shape)
        .background(
            brush = glassCardGradient(isDark = isDark, isPressed = isPressed),
            shape = shape
        )
        .border(
            width = borderWidth,
            brush = glassBorderGradient(isDark = isDark, isHighlighted = isHighlighted),
            shape = shape
        )

    val finalModifier = when {
        testTag != null && onClick != null -> {
            baseModifier
                .testTag(testTag)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick
                )
        }
        onClick != null -> {
            baseModifier.clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
        }
        testTag != null -> baseModifier.testTag(testTag)
        else -> baseModifier
    }

    Box(
        modifier = finalModifier,
        content = content
    )
}

/**
 * Circular / Rounded Glass Icon Button with specular border and soft glow feedback.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    isActive: Boolean = false,
    activeTint: Color = NyxPurpleLight,
    activeBackground: Color = NyxPurple.copy(alpha = 0.28f),
    buttonSize: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    testTag: String? = null
) {
    val isDark by ThemeManager.isDarkMode.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = tween(150),
        label = "glass_icon_btn_scale"
    )

    val effectiveTint = when {
        isActive -> activeTint
        tint != Color.Unspecified -> tint
        isDark -> DarkTextPrimary
        else -> LightTextPrimary
    }

    val effectiveBg = when {
        isActive -> activeBackground
        isPressed -> if (isDark) Color(0x2EFFFFFF) else Color(0xD0FFFFFF)
        isDark -> Color(0x16FFFFFF)
        else -> Color(0x80FFFFFF)
    }

    val effectiveBorder = when {
        isActive -> Brush.verticalGradient(listOf(NyxPurpleLight, NyxPurple.copy(alpha = 0.5f)))
        isDark -> Brush.verticalGradient(listOf(Color(0x35FFFFFF), Color(0x10FFFFFF)))
        else -> Brush.verticalGradient(listOf(Color(0x40A855F7), Color(0x15DCD6EF)))
    }

    val buttonMod = modifier
        .size(buttonSize)
        .scale(scale)
        .clip(CircleShape)
        .background(effectiveBg)
        .border(1.dp, effectiveBorder, CircleShape)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, radius = buttonSize / 2),
            onClick = onClick
        )

    val finalMod = if (testTag != null) buttonMod.testTag(testTag) else buttonMod

    Box(
        modifier = finalMod,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = effectiveTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Translucent Glass Category Chip.
 */
@Composable
fun GlassPillChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String? = null
) {
    val isDark by ThemeManager.isDarkMode.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }

    val bgBrush = if (selected) {
        Brush.horizontalGradient(listOf(NyxPurple, NyxPurpleLight))
    } else {
        if (isDark) {
            Brush.verticalGradient(listOf(Color(0x1CFFFFFF), Color(0x0CFFFFFF)))
        } else {
            Brush.verticalGradient(listOf(Color(0xE8FFFFFF), Color(0xD0EDE9FE)))
        }
    }

    val borderBrush = if (selected) {
        Brush.horizontalGradient(listOf(NyxPurpleLight, Color.White))
    } else {
        if (isDark) {
            Brush.verticalGradient(listOf(Color(0x35FFFFFF), NyxPurple.copy(alpha = 0.25f)))
        } else {
            Brush.verticalGradient(listOf(NyxPurple.copy(alpha = 0.4f), Color(0x20DCD6EF)))
        }
    }

    val textColor = when {
        selected -> Color.White
        isDark -> DarkTextPrimary.copy(alpha = 0.9f)
        else -> LightTextPrimary.copy(alpha = 0.9f)
    }

    val shape = RoundedCornerShape(22.dp)
    var boxModifier = modifier
        .clip(shape)
        .background(bgBrush, shape)
        .border(1.dp, borderBrush, shape)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, radius = 24.dp),
            onClick = onClick
        )
        .padding(horizontal = 16.dp, vertical = 9.dp)

    if (testTag != null) {
        boxModifier = boxModifier.testTag(testTag)
    }

    Row(
        modifier = boxModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.2.sp
        )
    }
}

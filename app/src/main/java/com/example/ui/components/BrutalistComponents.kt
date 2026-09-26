package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Neo-Brutalist Box with solid offset shadow and hard border.
 */
@Composable
fun BrutalCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BrutalWhite,
    borderColor: Color = BrutalBlack,
    shadowColor: Color = BrutalShadow,
    borderWidth: Dp = 3.dp,
    shadowOffset: Dp = 4.dp,
    shape: Shape = RectangleShape,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .drawBehind {
                if (shadowOffset > 0.dp) {
                    val pxOffset = shadowOffset.toPx()
                    drawRect(
                        color = shadowColor,
                        topLeft = Offset(pxOffset, pxOffset),
                        size = size
                    )
                }
            }
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape),
        content = content
    )
}

/**
 * Neo-Brutalist Button with tactile offset push animation.
 */
@Composable
fun BrutalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = BrutalYellow,
    contentColor: Color = BrutalBlack,
    borderColor: Color = BrutalBlack,
    borderWidth: Dp = 3.dp,
    shadowOffset: Dp = 4.dp,
    testTag: String = "brutal_button",
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentOffset by animateDpAsState(
        targetValue = if (isPressed) 0.dp else shadowOffset,
        label = "brutal_button_press"
    )

    val translation by animateDpAsState(
        targetValue = if (isPressed) shadowOffset else 0.dp,
        label = "brutal_button_translate"
    )

    Box(
        modifier = modifier
            .testTag(testTag)
            .offset(x = translation, y = translation)
            .drawBehind {
                if (currentOffset > 0.dp) {
                    val px = currentOffset.toPx()
                    drawRect(
                        color = borderColor,
                        topLeft = Offset(px, px),
                        size = size
                    )
                }
            }
            .background(if (enabled) backgroundColor else BrutalGrayLight)
            .border(borderWidth, borderColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * Sticker-style Brutalist Badge (e.g. NOW PLAYING, YT SOURCE, TRENDING).
 */
@Composable
fun BrutalBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = BrutalYellow,
    textColor: Color = BrutalBlack,
    borderColor: Color = BrutalBlack,
    borderWidth: Dp = 2.dp
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .border(borderWidth, borderColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Brutalist divider with thick stroke and custom pattern.
 */
@Composable
fun BrutalDivider(
    modifier: Modifier = Modifier,
    color: Color = BrutalBlack,
    thickness: Dp = 3.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}

/**
 * Monospaced track number indicator (e.g. #01, #02).
 */
@Composable
fun BrutalTrackNumber(
    number: Int,
    modifier: Modifier = Modifier,
    color: Color = BrutalBlack
) {
    val formatted = if (number < 10) "#0$number" else "#$number"
    Text(
        text = formatted,
        modifier = modifier,
        color = color,
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        letterSpacing = (-1).sp
    )
}

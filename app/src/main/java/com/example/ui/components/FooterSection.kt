package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun FooterSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BrutalBlack)
            .border(3.dp, BrutalBlack)
            .padding(16.dp)
    ) {
        // Tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BRUTAL BEATS // V1.0 RAW",
                color = BrutalYellow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            BrutalBadge(
                text = "YT COMPLIANT",
                backgroundColor = BrutalWhite,
                textColor = BrutalBlack
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "LEGAL & SOURCE DISCLOSURE:",
            color = BrutalWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "All music, audio, artwork, and video streams are served exclusively via official YouTube IFrame embed mechanisms. BRUTAL BEATS does not download, rip, modify, or re-host YouTube audio or video files. Content copyright remains with respective artists, labels, and YouTube uploaders. Fully compliant with YouTube API Services Terms of Service.",
            color = Color(0xFFBBBBBB),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))
        BrutalDivider(thickness = 1.dp, color = Color(0xFF444444))
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SYS: ANDROID JETPACK COMPOSE",
                color = Color(0xFF888888),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "ENGINE: YOUTUBE_IFRAME_API",
                color = Color(0xFF888888),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

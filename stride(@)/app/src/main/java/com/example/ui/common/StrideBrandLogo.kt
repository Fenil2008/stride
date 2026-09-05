package com.example.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.OffWhite

/**
 * Official Stride Brand Logo matching the uploaded visual identity:
 * GPS location pin integrated with a heartbeat ECG pulse waveform and bold athletic typography.
 */
@Composable
fun StrideBrandLogo(
    modifier: Modifier = Modifier,
    markSize: Dp = 48.dp,
    fontSize: TextUnit = 32.sp,
    accentColor: Color = ElectricCyan,
    textColor: Color = OffWhite,
    tagline: String? = null
) {
    Column(
        modifier = modifier.testTag("stride_brand_logo"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_stride_mark),
                contentDescription = "Stride Logo Mark",
                modifier = Modifier
                    .size(markSize)
                    .testTag("stride_logo_mark"),
                colorFilter = ColorFilter.tint(accentColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "STRIDE",
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 2.sp,
                color = textColor,
                modifier = Modifier.testTag("stride_logo_text")
            )
        }

        if (!tagline.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tagline.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                color = accentColor.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Compact Icon Mark for App Bars and Navigation
 */
@Composable
fun StrideMarkIcon(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = ElectricCyan
) {
    Image(
        painter = painterResource(id = R.drawable.ic_stride_mark),
        contentDescription = "Stride Mark",
        modifier = modifier
            .size(size)
            .testTag("stride_mark_icon"),
        colorFilter = ColorFilter.tint(tint)
    )
}

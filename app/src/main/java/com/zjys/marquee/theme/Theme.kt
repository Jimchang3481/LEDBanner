package com.zjys.marquee.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun 跑馬燈Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Color = Color(0xFF6750A4),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            val contentColor = if (ColorUtils.calculateLuminance(seedColor.toArgb()) > 0.6f) Color.Black else Color.White
            darkColorScheme(
                primary = seedColor,
                onPrimary = contentColor,
                primaryContainer = seedColor.copy(alpha = 0.3f),
                onPrimaryContainer = seedColor,
                secondary = seedColor.copy(alpha = 0.8f),
                secondaryContainer = seedColor.copy(alpha = 0.2f),
                onSecondaryContainer = seedColor,
                tertiary = seedColor,
                surface = Color(0xFF121212),
                background = Color(0xFF121212),
                surfaceVariant = Color(0xFF2B2B2B)
            )
        }
        else -> {
            val contentColor = if (ColorUtils.calculateLuminance(seedColor.toArgb()) > 0.5f) Color.Black else Color.White
            lightColorScheme(
                primary = seedColor,
                onPrimary = contentColor,
                primaryContainer = seedColor.copy(alpha = 0.2f),
                onPrimaryContainer = seedColor,
                secondary = seedColor.copy(alpha = 0.8f),
                secondaryContainer = seedColor.copy(alpha = 0.15f),
                onSecondaryContainer = seedColor,
                tertiary = seedColor,
                surface = Color(0xFFFBFBFB),
                background = Color(0xFFFBFBFB),
                surfaceVariant = Color(0xFFE2E2E2)
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

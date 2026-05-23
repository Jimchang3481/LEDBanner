package com.zjys.marquee.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat


@Composable
fun MarqueeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Color = Color(0xFF6750A4),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // 優先權 1：使用者開啟動態配色，且系統支援
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // 優先權 2：深色模式 (使用 seedColor 或預設色)
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
        // 優先權 3：淺色模式 (使用 seedColor 或預設色)
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

    // 系統狀態列與導覽列配色整合
    val view = LocalContext.current as? Activity
    if (view != null && !view.isFinishing) {
        SideEffect {
            val window = view.window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

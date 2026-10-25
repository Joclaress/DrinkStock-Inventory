package com.example.cokeinventory.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CokeRed,
    onPrimary = Color.White,
    primaryContainer = CokeRedBg,
    onPrimaryContainer = CokeDarkRed,
    secondary = CokeBlack,
    onSecondary = Color.White,
    background = CokeLightGray,
    onBackground = CokeBlack,
    surface = Color.White,
    onSurface = CokeBlack,
    surfaceVariant = CokeLightGray,
    onSurfaceVariant = CokeGray
)

private val DarkColorScheme = darkColorScheme(
    primary = CokeLightRed,
    onPrimary = Color.White,
    primaryContainer = CokeDarkRed,
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = CokeBlack,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color.LightGray
)

@Composable
fun SoftdrinksInventoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun CokeInventoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = SoftdrinksInventoryTheme(darkTheme, content)
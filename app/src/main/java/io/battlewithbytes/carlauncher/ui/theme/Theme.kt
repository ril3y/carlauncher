package io.battlewithbytes.carlauncher.ui.theme

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

// Tesla-inspired true black theme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3E6AE1), // Tesla blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Color(0xFF4CAF50), // Green for battery/good status
    onSecondary = Color.White,
    tertiary = Color(0xFFFF9800), // Orange for warnings
    background = Color(0xFF000000), // True black
    surface = Color(0xFF000000), // True black
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1A1A1A), // Very dark gray for subtle cards
    onSurfaceVariant = Color(0xFFB0B0B0),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    tertiary = Color(0xFF3700B3),
    background = Color(0xFFFFFBFE),
    surface = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun BattleWithBytesCarLauncherTheme(
    darkTheme: Boolean = true, // Always use dark theme for Tesla-style
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // Always use dark color scheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

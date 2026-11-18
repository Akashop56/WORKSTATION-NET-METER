package com.workstation.netmeter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.workstation.netmeter.settings.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF004B6E),
    onPrimaryContainer = Color(0xFFCDEBFF),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003733),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1F2630),
    onSurfaceVariant = Color(0xFFB6C2CF),
    outline = Color(0xFF30363D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006495),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEBFF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF00696B),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF8A5100),
    background = Color(0xFFF7FAFD),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E6EC),
    onSurfaceVariant = Color(0xFF41474D),
    outline = Color(0xFF71787F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

@Composable
fun NetMeterTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content,
    )
}

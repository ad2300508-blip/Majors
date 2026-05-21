package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ScholarBlue80,
    secondary = ScholarBlueMid80,
    tertiary = ScholarAccent80,
    background = Color(0xFF0F1117),
    surface = Color(0xFF1A1D27),
    surfaceVariant = Color(0xFF232737),
    onBackground = Color(0xFFE8EAF6),
    onSurface = Color(0xFFE8EAF6),
)

private val LightColorScheme = lightColorScheme(
    primary = ScholarBlue40,
    secondary = ScholarBlueMid40,
    tertiary = ScholarAccent40,
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF1FA),
    onBackground = Color(0xFF0F1117),
    onSurface = Color(0xFF1A1D27),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

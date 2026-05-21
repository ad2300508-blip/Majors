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
    primary         = ScholarBlue80,
    onPrimary       = Color(0xFF001F8A),
    primaryContainer= Color(0xFF0A2E9E),
    onPrimaryContainer = Color(0xFFDDE3FF),
    secondary       = ScholarBlueMid80,
    onSecondary     = Color(0xFF0D2247),
    secondaryContainer = Color(0xFF253F6A),
    onSecondaryContainer = Color(0xFFD3E4FF),
    tertiary        = ScholarAccent80,
    onTertiary      = Color(0xFF003828),
    tertiaryContainer = Color(0xFF005138),
    onTertiaryContainer = Color(0xFFB8EDD4),
    error           = Color(0xFFFFB4AB),
    onError         = Color(0xFF690005),
    errorContainer  = Color(0xFF93000A),
    onErrorContainer= Color(0xFFFFDAD6),
    background      = Surface0Dark,
    onBackground    = Color(0xFFE3E5F5),
    surface         = Surface1Dark,
    onSurface       = Color(0xFFE3E5F5),
    surfaceVariant  = Surface2Dark,
    onSurfaceVariant= Color(0xFFC2C6DC),
    outline         = Color(0xFF8B8FA8),
    outlineVariant  = Color(0xFF3A3E55),
)

private val LightColorScheme = lightColorScheme(
    primary         = ScholarBlue40,
    onPrimary       = Color.White,
    primaryContainer= Color(0xFFDDE3FF),
    onPrimaryContainer = Color(0xFF001460),
    secondary       = ScholarBlueMid40,
    onSecondary     = Color.White,
    secondaryContainer = Color(0xFFD3E4FF),
    onSecondaryContainer = Color(0xFF001C3A),
    tertiary        = ScholarAccent40,
    onTertiary      = Color.White,
    tertiaryContainer = Color(0xFFB8EDD4),
    onTertiaryContainer = Color(0xFF002115),
    background      = Surface0Light,
    onBackground    = Color(0xFF0F1117),
    surface         = Surface1Light,
    onSurface       = Color(0xFF1A1D27),
    surfaceVariant  = Surface2Light,
    onSurfaceVariant= Color(0xFF434759),
    outline         = Color(0xFF737893),
    outlineVariant  = Color(0xFFC3C6DC),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

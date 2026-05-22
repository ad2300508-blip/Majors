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

private val LightColorScheme = lightColorScheme(
    primary              = Indigo40,
    onPrimary            = Color.White,
    primaryContainer     = Indigo95,
    onPrimaryContainer   = Indigo10,
    secondary            = Violet40,
    onSecondary          = Color.White,
    secondaryContainer   = Violet90,
    onSecondaryContainer = Violet20,
    tertiary             = Teal40,
    onTertiary           = Color.White,
    tertiaryContainer    = Teal90,
    onTertiaryContainer  = Teal10,
    error                = ErrorRed,
    onError              = Color.White,
    errorContainer       = Color(0xFFFFE4E1),
    onErrorContainer     = Color(0xFF7A0000),
    background           = Paper,
    onBackground         = Color(0xFF1A1926),
    surface              = Paper2,
    onSurface            = Color(0xFF1A1926),
    surfaceVariant       = Paper3,
    onSurfaceVariant     = Color(0xFF4D4A5E),
    outline              = LineLight,
    outlineVariant       = Paper4,
    inverseSurface       = Paper2Dark,
    inverseOnSurface     = Color(0xFFF2EFE8),
    inversePrimary       = Indigo80,
)

private val DarkColorScheme = darkColorScheme(
    primary              = Indigo80,
    onPrimary            = PaperDark,
    primaryContainer     = Color(0xFF252550),
    onPrimaryContainer   = Indigo90,
    secondary            = Violet80,
    onSecondary          = Color(0xFF3A2000),
    secondaryContainer   = Color(0xFF3A2C10),
    onSecondaryContainer = Violet90,
    tertiary             = Teal80,
    onTertiary           = Color(0xFF0A2718),
    tertiaryContainer    = Color(0xFF1A3D2A),
    onTertiaryContainer  = Teal90,
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
    background           = PaperDark,
    onBackground         = Color(0xFFF2EFE8),
    surface              = Paper2Dark,
    onSurface            = Color(0xFFF2EFE8),
    surfaceVariant       = Paper3Dark,
    onSurfaceVariant     = Color(0xFFB9B6AF),
    outline              = LineDark,
    outlineVariant       = Paper4Dark,
    inverseSurface       = Paper2,
    inverseOnSurface     = Color(0xFF1A1926),
    inversePrimary       = Indigo40,
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
        else      -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

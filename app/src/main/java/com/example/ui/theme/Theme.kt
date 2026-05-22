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
    primary              = Indigo80,
    onPrimary            = Indigo20,
    primaryContainer     = Color(0xFF1E2D8B),
    onPrimaryContainer   = Indigo90,
    secondary            = Violet80,
    onSecondary          = Violet20,
    secondaryContainer   = Color(0xFF3B2887),
    onSecondaryContainer = Violet90,
    tertiary             = Teal80,
    onTertiary           = Teal10,
    tertiaryContainer    = Color(0xFF005048),
    onTertiaryContainer  = Teal90,
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
    background           = Neutral10,
    onBackground         = Color(0xFFE4E2EF),
    surface              = Neutral14,
    onSurface            = Color(0xFFE4E2EF),
    surfaceVariant       = Neutral18,
    onSurfaceVariant     = Color(0xFFC9C5D4),
    outline              = Color(0xFF938F9F),
    outlineVariant       = Color(0xFF3A3748),
    inverseSurface       = Color(0xFFE4E2EF),
    inverseOnSurface     = Neutral18,
    inversePrimary       = Indigo40,
)

private val LightColorScheme = lightColorScheme(
    primary              = Indigo40,
    onPrimary            = Color.White,
    primaryContainer     = Indigo90,
    onPrimaryContainer   = Indigo10,
    secondary            = Violet40,
    onSecondary          = Color.White,
    secondaryContainer   = Violet90,
    onSecondaryContainer = Violet20,
    tertiary             = Teal40,
    onTertiary           = Color.White,
    tertiaryContainer    = Teal90,
    onTertiaryContainer  = Teal10,
    background           = Neutral98,
    onBackground         = Color(0xFF1A1826),
    surface              = Neutral99,
    onSurface            = Color(0xFF1A1826),
    surfaceVariant       = Neutral96,
    onSurfaceVariant     = Color(0xFF47444F),
    outline              = Color(0xFF79757F),
    outlineVariant       = Neutral87,
    inverseSurface       = Neutral18,
    inverseOnSurface     = Color(0xFFE4E2EF),
    inversePrimary       = Indigo80,
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

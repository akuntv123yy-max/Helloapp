package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = HalloDarkPrimary,
    onPrimary = HalloDarkOnPrimary,
    primaryContainer = HalloDarkPrimaryContainer,
    onPrimaryContainer = HalloDarkOnPrimaryContainer,
    secondary = HalloDarkSecondary,
    secondaryContainer = HalloDarkSecondaryContainer,
    onSecondaryContainer = HalloDarkOnSecondaryContainer,
    background = HalloDarkBackground,
    onBackground = HalloDarkOnBackground,
    surface = HalloDarkSurface,
    onSurface = HalloDarkOnSurface,
    surfaceVariant = HalloDarkSurfaceVariant,
    onSurfaceVariant = HalloDarkOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = HalloLightPrimary,
    onPrimary = HalloLightOnPrimary,
    primaryContainer = HalloLightPrimaryContainer,
    onPrimaryContainer = HalloLightOnPrimaryContainer,
    secondary = HalloLightSecondary,
    secondaryContainer = HalloLightSecondaryContainer,
    onSecondaryContainer = HalloLightOnSecondaryContainer,
    background = HalloLightBackground,
    onBackground = HalloLightOnBackground,
    surface = HalloLightSurface,
    onSurface = HalloLightOnSurface,
    surfaceVariant = HalloLightSurfaceVariant,
    onSurfaceVariant = HalloLightOnSurfaceVariant
)

@Composable
fun HalloappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep brand cyan/blue identity by default
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

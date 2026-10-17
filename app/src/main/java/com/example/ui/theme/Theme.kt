package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalAccentColor = staticCompositionLocalOf { "indigo" }

fun getAccentColorScheme(accent: String, isDark: Boolean): ColorScheme {
    val (primary, onPrimary, container, onContainer) = when (accent) {
        "emerald" -> listOf(EmeraldPrimary, EmeraldOnPrimary, EmeraldPrimaryContainer, EmeraldOnPrimaryContainer)
        "rose" -> listOf(RosePrimary, RoseOnPrimary, RosePrimaryContainer, RoseOnPrimaryContainer)
        "amber" -> listOf(AmberPrimary, AmberOnPrimary, AmberPrimaryContainer, AmberOnPrimaryContainer)
        "violet" -> listOf(VioletPrimary, VioletOnPrimary, VioletPrimaryContainer, VioletOnPrimaryContainer)
        "ocean" -> listOf(OceanPrimary, OceanOnPrimary, OceanPrimaryContainer, OceanOnPrimaryContainer)
        else -> listOf(IndigoPrimary, IndigoOnPrimary, IndigoPrimaryContainer, IndigoOnPrimaryContainer)
    }

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            background = Gray950,
            surface = Gray900,
            onBackground = Gray100,
            onSurface = Gray100,
            surfaceVariant = Gray800,
            onSurfaceVariant = Gray300,
            outline = Gray700
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            background = Gray50,
            surface = Color.White,
            onBackground = Gray900,
            onSurface = Gray900,
            surfaceVariant = Gray100,
            onSurfaceVariant = Gray600,
            outline = Gray300
        )
    }
}

@Composable
fun NotelyTheme(
    themeMode: String = "system", // "system", "light", "dark"
    accentColor: String = "indigo",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemInDark
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getAccentColorScheme(accentColor, isDark)
    }

    CompositionLocalProvider(LocalAccentColor provides accentColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

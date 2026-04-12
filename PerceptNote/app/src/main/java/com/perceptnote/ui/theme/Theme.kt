// 📄 app/src/main/java/com/perceptnote/ui/theme/Theme.kt
package com.perceptnote.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PrimaryContainer80,
    onPrimaryContainer = PrimaryContainer40,
    secondary = Secondary40,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = SecondaryContainer80,
    onSecondaryContainer = SecondaryContainer40,
    tertiary = Tertiary40,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
)

// CompositionLocal pour le mode dyslexie (EchoesClass)
val LocalDyslexiaMode = staticCompositionLocalOf { false }

@Composable
fun PerceptNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dyslexiaMode: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dyslexiaMode -> lightColorScheme(
            primary = Primary40,
            background = DyslexiaBackground,
            surface = DyslexiaSurface,
            onBackground = DyslexiaText,
            onSurface = DyslexiaText
        )
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val typography = if (dyslexiaMode) DyslexiaTypography else PerceptNoteTypography

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalDyslexiaMode provides dyslexiaMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

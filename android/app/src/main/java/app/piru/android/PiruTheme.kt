package app.piru.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3 darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PiruLightColors = lightColorScheme(
    primary = Color(red = 0.898f, green = 0.497f, blue = 0.591f),
    onPrimary = Color.White,
    background = Color(red = 0.97f, green = 0.97f, blue = 0.98f),
    surface = Color.White,
    surfaceVariant = Color(red = 0.94f, green = 0.93f, blue = 0.95f),
    onSurfaceVariant = Color(red = 0.43f, green = 0.43f, blue = 0.45f)
)

private val PiruDarkColors = darkColorScheme(
    primary = Color(red = 0.920f, green = 0.268f, blue = 0.441f),
    onPrimary = Color.White,
    background = Color.Black,
    surface = Color(red = 0.14f, green = 0.14f, blue = 0.15f),
    surfaceVariant = Color(red = 0.20f, green = 0.19f, blue = 0.21f),
    onSurfaceVariant = Color(red = 0.82f, green = 0.80f, blue = 0.82f)
)

@Composable
fun PiruTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) PiruDarkColors else PiruLightColors,
        typography = Typography(),
        content = content
    )
}

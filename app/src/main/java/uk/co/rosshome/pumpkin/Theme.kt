package uk.co.rosshome.pumpkin

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PumpkinOrange = Color(0xFFF28C28)
private val PumpkinDeep = Color(0xFFC45B00)
private val PumpkinGreen = Color(0xFF3D7B3D)
private val PumpkinCream = Color(0xFFFFF4E6)
private val PumpkinBrown = Color(0xFF4A2F1A)

private val LightColors = lightColorScheme(
    primary = PumpkinOrange,
    onPrimary = Color.White,
    secondary = PumpkinGreen,
    onSecondary = Color.White,
    tertiary = PumpkinDeep,
    onTertiary = Color.White,
    background = PumpkinCream,
    onBackground = PumpkinBrown,
    surface = Color(0xFFFFF8F0),
    onSurface = PumpkinBrown,
)

private val DarkColors = darkColorScheme(
    primary = PumpkinOrange,
    onPrimary = Color.Black,
    secondary = PumpkinGreen,
    onSecondary = Color.Black,
    tertiary = PumpkinDeep,
    onTertiary = Color.White,
    background = Color(0xFF1E140C),
    onBackground = Color(0xFFFFE6C8),
    surface = Color(0xFF2A1B10),
    onSurface = Color(0xFFFFE6C8),
)

@Composable
fun PumpkinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) {
            DarkColors
        } else {
            LightColors
        },
        content = content,
    )
}

package ru.ekrupin.ivi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Caramel,
    onPrimary = WarmWhite,
    background = WarmWhite,
    onBackground = BrownText,
    surface = Cream,
    onSurface = BrownText,
    surfaceVariant = SoftSurface,
    secondary = SoftRose,
)

private val DarkColors = darkColorScheme(
    primary = SoftRose,
    onPrimary = BrownText,
    background = BrownText,
    onBackground = WarmWhite,
    surface = CaramelDark,
    onSurface = WarmWhite,
)

@Composable
fun IviTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content,
    )
}

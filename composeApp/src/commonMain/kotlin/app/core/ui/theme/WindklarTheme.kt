package app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class WindklarColors(
    val screenBackground: Color,
    val primaryGreen: Color,
    val headerEndGreen: Color,
    val darkGreen: Color,
    val mutedGreen: Color,
    val paleGreen: Color,
    val cardBackground: Color,
    val dividerColor: Color
)

val LightWindklarColors = WindklarColors(
    screenBackground = Color(0xFFF8FAF7),
    primaryGreen = Color(0xFF2D5A2D),
    headerEndGreen = Color(0xFF43A047),
    darkGreen = Color(0xFF1A3A1A),
    mutedGreen = Color(0xFF5A7A5A),
    paleGreen = Color(0xFFE8F5E9),
    cardBackground = Color.White,
    dividerColor = Color(0xFFE8F5E9)
)

@Immutable
data class WindklarSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp
)

@Immutable
data class WindklarRadii(
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp
)

@Immutable
data class WindklarElevation(
    val none: Dp = 0.dp,
    val card: Dp = 4.dp,
    val dialog: Dp = 8.dp
)

val LocalWindklarColors = staticCompositionLocalOf { LightWindklarColors }
val LocalWindklarSpacing = staticCompositionLocalOf { WindklarSpacing() }
val LocalWindklarRadii = staticCompositionLocalOf { WindklarRadii() }
val LocalWindklarElevation = staticCompositionLocalOf { WindklarElevation() }

object WindklarTheme {
    val colors: WindklarColors
        @Composable
        get() = LocalWindklarColors.current

    val spacing: WindklarSpacing
        @Composable
        get() = LocalWindklarSpacing.current

    val radii: WindklarRadii
        @Composable
        get() = LocalWindklarRadii.current

    val elevation: WindklarElevation
        @Composable
        get() = LocalWindklarElevation.current
}

@Composable
fun WindklarTheme(
    content: @Composable () -> Unit
) {
    val colors = LightWindklarColors
    val materialColorScheme = lightColorScheme(
        primary = colors.primaryGreen,
        background = colors.screenBackground,
        onBackground = colors.darkGreen,
        surface = colors.cardBackground,
        onSurface = colors.darkGreen
    )

    CompositionLocalProvider(
        LocalWindklarColors provides colors,
        LocalWindklarSpacing provides WindklarSpacing(),
        LocalWindklarRadii provides WindklarRadii(),
        LocalWindklarElevation provides WindklarElevation()
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content
        )
    }
}

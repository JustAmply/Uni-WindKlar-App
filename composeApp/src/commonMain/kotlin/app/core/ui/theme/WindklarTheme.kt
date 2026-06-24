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
    val dividerColor: Color,
    val trackGreen: Color,
    val heartRed: Color,
    val errorRed: Color,
    val errorDarkRed: Color,
    val warningYellowLight: Color,
    val warningAmber: Color,
    val warningAmberDark: Color,
    val warningBrown: Color,
    val gray: Color,
    val darkText: Color,
    val mutedText: Color,
    val contactCardEndGreen: Color,
    val lightOverlayGreen: Color,
    val statusOrangeLight: Color,
    val statusOrangeDark: Color,
    val turbineTeal: Color,
    val impactToneContainer2: Color,
    val impactToneIcon2: Color,
    val impactToneContainer3: Color,
    val impactToneAccent3: Color,
    val impactToneIcon3: Color,
    val impactToneIcon4: Color,
    val startGradientStop1: Color,
    val startGradientStop2: Color,
    val startGradientStop3: Color,
    val startGradientStop4: Color,
    val qualityOfficialContent: Color,
    val qualityDerivedContainer: Color,
    val qualityDerivedContent: Color,
    val qualityEstimatedContainer: Color,
    val qualityEstimatedContent: Color,
    val qualityMissingContainer: Color,
    val qualityMissingContent: Color,
    val primaryButtonContainer: Color,
    val primaryButtonDisabledContainer: Color,
    val primaryButtonDisabledContent: Color,
)

val LightWindklarColors = WindklarColors(
    screenBackground = Color(0xFFF8FAF7),
    primaryGreen = Color(0xFF2D5A2D),
    headerEndGreen = Color(0xFF43A047),
    darkGreen = Color(0xFF1A3A1A),
    mutedGreen = Color(0xFF5A7A5A),
    paleGreen = Color(0xFFE8F5E9),
    cardBackground = Color.White,
    dividerColor = Color(0xFFE8F5E9),
    trackGreen = Color(0xFFDDEBDD),
    heartRed = Color(0xFFE53935),
    errorRed = Color(0xFFD32F2F),
    errorDarkRed = Color(0xFF5C1D1D),
    warningYellowLight = Color(0xFFFFF9C4),
    warningAmber = Color(0xFFFBC02D),
    warningAmberDark = Color(0xFFF57F17),
    warningBrown = Color(0xFF5D4037),
    gray = Color(0xFF757575),
    darkText = Color(0xFF17261A),
    mutedText = Color(0xFF647568),
    contactCardEndGreen = Color(0xFFC8E6C9),
    lightOverlayGreen = Color(0xFFD8E7D8),
    statusOrangeLight = Color(0xFFFFF3E0),
    statusOrangeDark = Color(0xFFE65100),
    turbineTeal = Color(0xFF009688),
    impactToneContainer2 = Color(0xFFF4FAF1),
    impactToneIcon2 = Color(0xFFDDF1DA),
    impactToneContainer3 = Color(0xFFF7FBF4),
    impactToneAccent3 = Color(0xFF2F6B45),
    impactToneIcon3 = Color(0xFFE1F2DE),
    impactToneIcon4 = Color(0xFFEAF4E7),
    startGradientStop1 = Color(0xD907170F),
    startGradientStop2 = Color(0x9E4E7B2D),
    startGradientStop3 = Color(0xB277A03A),
    startGradientStop4 = Color(0xCC234E25),
    qualityOfficialContent = Color(0xFF1B5E20),
    qualityDerivedContainer = Color(0xFFD5F0EA),
    qualityDerivedContent = Color(0xFF00695C),
    qualityEstimatedContainer = Color(0xFFFFECB3),
    qualityEstimatedContent = Color(0xFF8A5A00),
    qualityMissingContainer = Color(0xFFE0E0E0),
    qualityMissingContent = Color(0xFF616161),
    primaryButtonContainer = Color(0xFFE6E8E5),
    primaryButtonDisabledContainer = Color(0xFFB7BCB6),
    primaryButtonDisabledContent = Color(0xFF4C594C),
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

fun Color.toHexRgb(): String {
    val r = (red * 255f + 0.5f).toInt().coerceIn(0, 255)
    val g = (green * 255f + 0.5f).toInt().coerceIn(0, 255)
    val b = (blue * 255f + 0.5f).toInt().coerceIn(0, 255)
    return "#" + r.toString(16).padStart(2, '0').uppercase() +
        g.toString(16).padStart(2, '0').uppercase() +
        b.toString(16).padStart(2, '0').uppercase()
}

fun Color.toHexArgb(): String {
    val a = (alpha * 255f + 0.5f).toInt().coerceIn(0, 255)
    val r = (red * 255f + 0.5f).toInt().coerceIn(0, 255)
    val g = (green * 255f + 0.5f).toInt().coerceIn(0, 255)
    val b = (blue * 255f + 0.5f).toInt().coerceIn(0, 255)
    return "#" + a.toString(16).padStart(2, '0').uppercase() +
        r.toString(16).padStart(2, '0').uppercase() +
        g.toString(16).padStart(2, '0').uppercase() +
        b.toString(16).padStart(2, '0').uppercase()
}

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

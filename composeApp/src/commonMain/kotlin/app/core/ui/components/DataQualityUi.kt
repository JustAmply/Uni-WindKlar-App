package app.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.core.ui.theme.WindklarTheme

data class DataQualityColors(
    val container: Color,
    val content: Color,
)

fun formatDataQuality(quality: String): String = when (quality.lowercase()) {
    "official" -> "Offiziell"
    "measured" -> "Gemessen"
    "derived" -> "Abgeleitet"
    "estimated" -> "Geschätzt"
    "simulated" -> "Simuliert"
    "missing" -> "Fehlend"
    else -> quality.ifBlank { "Unbekannt" }
}

@Composable
fun qualityColors(quality: String): DataQualityColors {
    val colors = WindklarTheme.colors
    return when (quality.lowercase()) {
        "official", "measured" -> {
            DataQualityColors(
                container = colors.contactCardEndGreen,
                content = colors.qualityOfficialContent,
            )
        }
        "derived" -> {
            DataQualityColors(
                container = colors.qualityDerivedContainer,
                content = colors.qualityDerivedContent,
            )
        }
        "estimated", "simulated" -> {
            DataQualityColors(
                container = colors.qualityEstimatedContainer,
                content = colors.qualityEstimatedContent,
            )
        }
        "missing" -> {
            DataQualityColors(
                container = colors.qualityMissingContainer,
                content = colors.qualityMissingContent,
            )
        }
        else -> {
            DataQualityColors(
                container = colors.paleGreen,
                content = colors.primaryGreen,
            )
        }
    }
}

/**
 * A full-width row that places [label] on the left (weight 1f) and a data-quality
 * badge on the right. Prevents the badge from wrapping vertically when the label is long.
 *
 * @param label      The section or metric label shown in dark text.
 * @param quality    A data-quality string from the domain model (e.g. "estimated").
 * @param labelColor Text color for the label. Defaults to dark green.
 * @param labelSize  Font size for the label in sp. Defaults to 14.
 */
@Composable
fun LabelWithBadge(
    label: String,
    quality: String,
    labelColor: Color = WindklarTheme.colors.darkGreen,
    labelSize: Int = 14,
) {
    val qualityColor = qualityColors(quality)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = labelSize.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        StatusBadge(
            text = formatDataQuality(quality).uppercase(),
            containerColor = qualityColor.container,
            contentColor = qualityColor.content,
        )
    }
}

/**
 * A small pill-shaped badge with arbitrary container and content colors.
 * Use this for non-quality badges such as turbine operating status.
 *
 * @param text           Label shown inside the badge.
 * @param containerColor Background color of the badge.
 * @param contentColor   Text color inside the badge.
 */
@Composable
fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

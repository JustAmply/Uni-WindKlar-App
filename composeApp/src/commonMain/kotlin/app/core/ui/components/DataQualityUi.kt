package app.core.ui.components

import androidx.compose.ui.graphics.Color

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

fun qualityColors(quality: String): DataQualityColors = when (quality.lowercase()) {
    "official", "measured" -> DataQualityColors(
        container = Color(0xFFC8E6C9),
        content = Color(0xFF1B5E20),
    )
    "derived" -> DataQualityColors(
        container = Color(0xFFD5F0EA),
        content = Color(0xFF00695C),
    )
    "estimated", "simulated" -> DataQualityColors(
        container = Color(0xFFFFECB3),
        content = Color(0xFF8A5A00),
    )
    "missing" -> DataQualityColors(
        container = Color(0xFFE0E0E0),
        content = Color(0xFF616161),
    )
    else -> DataQualityColors(
        container = Color(0xFFE8F5E9),
        content = Color(0xFF2D5A2D),
    )
}

package app.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.core.ui.theme.WindklarTheme
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DataHintDialog(
    latitude: Double?,
    longitude: Double?,
    contextLabel: String,
    defaultCategory: String = "wrong_technical_data",
    onDismiss: () -> Unit,
    onSubmit: (category: String, confidence: String, description: String, suggestedValue: String?) -> Unit
) {
    val categories = listOf(
        "missing_installation" to "Fehlende Windanlage",
        "wrong_location" to "Falscher Standort",
        "wrong_status" to "Falscher Status",
        "wrong_wind_park_assignment" to "Falscher Windpark",
        "wrong_technical_data" to "Falsche technische Daten",
        "installation_removed" to "Windanlage entfernt",
        "other" to "Sonstiges"
    )
    
    val confidences = listOf(
        "unsure" to "Unsicher",
        "likely" to "Wahrscheinlich",
        "certain" to "Sicher"
    )
    
    var selectedCategory by remember { mutableStateOf(defaultCategory) }
    var selectedConfidence by remember { mutableStateOf("likely") }
    var description by remember { mutableStateOf("") }
    var suggestedValue by remember { mutableStateOf("") }
    var descriptionTouched by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Datenhinweis erfassen",
                color = WindklarTheme.colors.darkGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Helfen Sie mit, die Datenqualität zu verbessern. Ihr Datenhinweis wird lokal gespeichert.",
                    color = WindklarTheme.colors.mutedGreen,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Text(
                    text = "Bezug: $contextLabel",
                    color = WindklarTheme.colors.darkGreen.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
                
                // Categories
                Column {
                    Text(
                        text = "Kategorie *",
                        color = WindklarTheme.colors.darkGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { (key, value) ->
                            val isSelected = selectedCategory == key
                            Surface(
                                onClick = { selectedCategory = key },
                                shape = CircleShape,
                                color = if (isSelected) WindklarTheme.colors.primaryGreen else WindklarTheme.colors.paleGreen,
                            ) {
                                Text(
                                    text = value,
                                    color = if (isSelected) Color.White else WindklarTheme.colors.primaryGreen,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
                
                // Suggested Value
                OutlinedTextField(
                    value = suggestedValue,
                    onValueChange = { suggestedValue = it },
                    label = { Text("Vorgeschlagener Wert (optional)") },
                    placeholder = { Text("z.B. Nabenhöhe 120m statt 100m") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Description
                val isDescriptionError = description.isBlank() && descriptionTouched
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionTouched = true
                    },
                    label = { Text("Beschreibung *") },
                    placeholder = { Text("Bitte beschreiben Sie den Fehler genauer...") },
                    isError = isDescriptionError,
                    supportingText = if (isDescriptionError) {
                        { Text("Beschreibung ist erforderlich") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                // Confidence
                Column {
                    Text(
                        text = "Hinweissicherheit *",
                        color = WindklarTheme.colors.darkGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        confidences.forEach { (key, value) ->
                            val isSelected = selectedConfidence == key
                            Surface(
                                onClick = { selectedConfidence = key },
                                shape = CircleShape,
                                color = if (isSelected) WindklarTheme.colors.primaryGreen else WindklarTheme.colors.paleGreen,
                            ) {
                                Text(
                                    text = value,
                                    color = if (isSelected) Color.White else WindklarTheme.colors.primaryGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                
                // Coordinates info
                if (latitude != null && longitude != null) {
                    Text(
                        text = "Gemeldete Koordinaten: ${latitude.roundTo(5)}° N, ${longitude.roundTo(5)}° O",
                        color = WindklarTheme.colors.mutedGreen,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (description.isNotBlank()) {
                        onSubmit(
                            selectedCategory,
                            selectedConfidence,
                            description,
                            suggestedValue.ifBlank { null }
                        )
                    }
                },
                enabled = description.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WindklarTheme.colors.primaryGreen,
                    contentColor = Color.White,
                    disabledContainerColor = WindklarTheme.colors.primaryButtonDisabledContainer,
                    disabledContentColor = WindklarTheme.colors.primaryButtonDisabledContent
                )
            ) {
                Text("Datenhinweis speichern")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = WindklarTheme.colors.primaryGreen
                )
            ) {
                Text("Abbrechen")
            }
        }
    )
}

private fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

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
import androidx.compose.ui.unit.sp

private val PrimaryGreen = Color(0xFF2D5A2D)
private val DarkGreen = Color(0xFF1A3A1A)
private val MutedGreen = Color(0xFF5A7A5A)
private val PaleGreen = Color(0xFFE8F5E9)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportWindTurbineDialog(
    currentLatitude: Double,
    currentLongitude: Double,
    onDismiss: () -> Unit,
    onSubmit: (category: String, confidence: String, description: String, suggestedValue: String?) -> Unit
) {
    val categories = listOf(
        "missing_installation" to "Fehlende Anlage",
        "wrong_location" to "Falscher Standort",
        "wrong_status" to "Falscher Status",
        "wrong_wind_park_assignment" to "Falscher Windpark",
        "wrong_technical_data" to "Falsche technische Daten",
        "installation_removed" to "Anlage entfernt",
        "other" to "Sonstiges"
    )
    
    val confidences = listOf(
        "unsure" to "Unsicher",
        "likely" to "Wahrscheinlich",
        "certain" to "Sicher"
    )
    
    var selectedCategory by remember { mutableStateOf("missing_installation") }
    var selectedConfidence by remember { mutableStateOf("likely") }
    var description by remember { mutableStateOf("") }
    var suggestedValue by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Datenhinweis senden",
                color = DarkGreen,
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
                    text = "Helfen Sie mit, die Datenqualität zu verbessern. Ihr Hinweis wird lokal für den Review gespeichert.",
                    color = MutedGreen,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                
                // Categories
                Column {
                    Text(
                        text = "Kategorie *",
                        color = DarkGreen,
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
                                shape = CircleShape,
                                color = if (isSelected) PrimaryGreen else PaleGreen,
                                modifier = Modifier.clickable { selectedCategory = key }
                            ) {
                                Text(
                                    text = value,
                                    color = if (isSelected) Color.White else PrimaryGreen,
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
                    label = { Text("Korrekturvorschlag (optional)") },
                    placeholder = { Text("z.B. Nabenhöhe 120m statt 100m") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung *") },
                    placeholder = { Text("Bitte beschreiben Sie den Fehler genauer...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                // Confidence
                Column {
                    Text(
                        text = "Zuversichtlichkeit *",
                        color = DarkGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        confidences.forEach { (key, value) ->
                            val isSelected = selectedConfidence == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) PrimaryGreen else PaleGreen,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedConfidence = key }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = value,
                                    color = if (isSelected) Color.White else PrimaryGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                // Coordinates info
                Text(
                    text = "Gemeldete Koordinaten: ${currentLatitude.roundTo(5)}° N, ${currentLongitude.roundTo(5)}° O",
                    color = MutedGreen,
                    fontSize = 12.sp
                )
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Absenden")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = PrimaryGreen
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

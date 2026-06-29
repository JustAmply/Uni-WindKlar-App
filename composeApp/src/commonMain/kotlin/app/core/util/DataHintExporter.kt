package app.core.util

import app.core.model.DataHint

fun List<DataHint>.toCsv(): String {
    val header = "id,category,confidence,status,description,windTurbineId,windParkId,municipalityId,latitude,longitude,suggestedValue,imageUri,createdAt,updatedAt"
    val rows = map { hint ->
        listOf(
            hint.id,
            hint.category,
            hint.confidence,
            hint.status,
            hint.description,
            hint.windTurbineId.orEmpty(),
            hint.windParkId.orEmpty(),
            hint.municipalityId.orEmpty(),
            hint.latitude?.toString().orEmpty(),
            hint.longitude?.toString().orEmpty(),
            hint.suggestedValue.orEmpty(),
            hint.imageUri.orEmpty(),
            hint.createdAtEpochMillis.toString(),
            hint.updatedAtEpochMillis.toString()
        ).joinToString(",") { field ->
            val clean = field.replace("\"", "\"\"")
            if (clean.contains(",") || clean.contains("\n") || clean.contains("\"")) {
                "\"$clean\""
            } else {
                clean
            }
        }
    }
    return (listOf(header) + rows).joinToString("\n")
}

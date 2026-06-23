package app.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.core.model.DataHint
import app.data.repository.WindParkRepository
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: WindParkRepository) : ViewModel() {
    var uiState: ProfileUiState by mutableStateOf(ProfileUiState())
        private set

    init {
        loadProfileState()
    }

    private fun loadProfileState() {
        viewModelScope.launch {
            val attribution = repository.getSnapshotAttribution()
            val limitations = repository.getSnapshotLimitations()
            val isOffshoreEnabled = repository.isOffshoreEnabled()
            uiState = uiState.copy(
                attribution = attribution,
                limitations = limitations,
                isOffshoreEnabled = isOffshoreEnabled,
            )
        }
    }

    fun toggleOffshoreEnabled() {
        setOffshoreEnabled(!uiState.isOffshoreEnabled)
    }

    fun setOffshoreEnabled(enabled: Boolean) {
        if (uiState.isOffshoreEnabled == enabled) return
        viewModelScope.launch {
            repository.setOffshoreEnabled(enabled)
            uiState = uiState.copy(isOffshoreEnabled = enabled)
        }
    }

    fun clearRecentWindParks(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.clearRecentWindParks()
            onSuccess()
        }
    }

    fun exportDataHints(onExport: (String) -> Unit) {
        viewModelScope.launch {
            val hints = repository.getDataHints()
            val csv = hints.toCsv()
            onExport(csv)
        }
    }

    private fun List<DataHint>.toCsv(): String {
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
}

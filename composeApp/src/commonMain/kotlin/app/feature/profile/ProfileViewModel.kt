package app.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.core.model.DataHint
import app.data.repository.ProfileRepository
import kotlinx.coroutines.launch
import app.core.util.toCsv

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {
    var uiState: ProfileUiState by mutableStateOf(ProfileUiState())
        private set

    init {
        loadProfileState()
    }

    private fun loadProfileState() {
        viewModelScope.launch {
            val attribution = repository.getSnapshotAttribution()
            val limitations = repository.getSnapshotLimitations()
            uiState = uiState.copy(
                attribution = attribution,
                limitations = limitations,
            )
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
}

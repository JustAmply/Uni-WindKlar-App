package app.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.data.repository.WindParkRepository
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: WindParkRepository) : ViewModel() {
    var uiState: ProfileUiState by mutableStateOf(ProfileUiState())
        private set

    init {
        loadMetadata()
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            val attribution = repository.getSnapshotAttribution()
            val limitations = repository.getSnapshotLimitations()
            uiState = uiState.copy(
                attribution = attribution,
                limitations = limitations
            )
        }
    }
}

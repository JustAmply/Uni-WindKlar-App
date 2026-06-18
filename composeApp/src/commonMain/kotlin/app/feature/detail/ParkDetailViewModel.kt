package app.feature.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.data.repository.WindParkRepository
import kotlinx.coroutines.launch

class ParkDetailViewModel(
    val parkId: String,
    private val repository: WindParkRepository,
) : ViewModel() {
    var uiState by mutableStateOf(ParkDetailUiState(parkId = parkId))
        private set

    init {
        loadParkDetails()
    }

    private fun loadParkDetails() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            
            repository.recordRecentWindPark(parkId)
            
            val park = repository.getWindPark(parkId)
            val turbines = repository.getWindTurbinesForPark(parkId)
            val metrics = repository.getMetricsForPark(parkId)
            val isFav = repository.isFavorite(parkId)
            val attribution = repository.getSnapshotAttribution()
            
            uiState = uiState.copy(
                isLoading = false,
                park = park,
                turbines = turbines,
                metrics = metrics,
                isFavorite = isFav,
                attribution = attribution
            )
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val nextFav = !uiState.isFavorite
            repository.setFavorite(parkId, nextFav)
            uiState = uiState.copy(isFavorite = nextFav)
        }
    }
}

package app.feature.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.core.model.ProductionContext
import app.data.repository.DataHintRepository
import app.data.repository.ParkDetailRepository
import kotlinx.coroutines.launch

class ParkDetailViewModel(
    val parkId: String,
    private val repository: ParkDetailRepository,
    private val dataHintRepository: DataHintRepository,
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
            val baseAssumptions = repository.getSnapshotAssumptions()
            val isFav = repository.isFavorite(parkId)
            val attribution = repository.getSnapshotAttribution()
            
            val capacityKw = park?.installedCapacityKw?.toDouble() ?: 0.0
            val productionKwh = metrics.firstOrNull { it.metricType == "annual_production" }?.value ?: 0.0
            val parkFullLoadHours = ProductionContext.fullLoadHours(
                annualProductionKwh = productionKwh,
                installedCapacityKw = capacityKw,
            )
            val assumptions = ProductionContext.assumptionsWithCalculatedFullLoadHours(
                assumptions = baseAssumptions,
                fullLoadHours = parkFullLoadHours,
                calculationNote = "Aus der Jahresproduktion dieses Windparks und seiner installierten Gesamtleistung berechnet. Der Wert macht die im Datensatz implizit verwendeten standort- und windanlagenspezifischen Ertragsannahmen sichtbar; bundesweiter Richtwert: 2.000 h/a.",
            )

            uiState = uiState.copy(
                isLoading = false,
                park = park,
                turbines = turbines,
                metrics = metrics,
                assumptions = assumptions,
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

    fun submitParkDataHint(
        category: String,
        confidence: String,
        description: String,
        suggestedValue: String?,
        onSuccess: () -> Unit,
    ) {
        val park = uiState.park ?: return
        viewModelScope.launch {
            dataHintRepository.submitDataHint(
                category = category,
                confidence = confidence,
                description = description,
                status = "ready_for_review",
                windTurbineId = null,
                windParkId = park.id,
                municipalityId = park.municipalityId,
                latitude = park.latitude,
                longitude = park.longitude,
                suggestedValue = suggestedValue,
                imageUri = null,
            )
            onSuccess()
        }
    }

}

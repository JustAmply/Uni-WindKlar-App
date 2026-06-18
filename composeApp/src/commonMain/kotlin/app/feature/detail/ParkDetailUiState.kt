package app.feature.detail

import app.core.model.WindPark
import app.core.model.WindTurbine
import app.core.model.Metric

data class ParkDetailUiState(
    val parkId: String,
    val isLoading: Boolean = true,
    val park: WindPark? = null,
    val turbines: List<WindTurbine> = emptyList(),
    val metrics: List<Metric> = emptyList(),
    val isFavorite: Boolean = false,
    val attribution: String = "",
)


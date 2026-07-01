package app.feature.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.data.repository.SavedPlacesRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

import app.core.util.formatGermanNumber
import app.core.model.WindPark
import app.core.model.Metric

class FavoritesViewModel(private val repository: SavedPlacesRepository) : ViewModel() {
    var uiState: FavoritesUiState by mutableStateOf(FavoritesUiState())
        private set

    private var hasLoaded = false
    private var isLoading = false
    private var loadRequestId = 0

    fun loadData(force: Boolean = false) {
        if (!force && hasLoaded) return
        val requestId = ++loadRequestId
        isLoading = true
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val favs = repository.getFavoriteWindParks()
                val recents = repository.getRecentWindParks(5)
                val favRegionSummaries = repository.getFavoriteRegionSummaries()

                // Batch load all metrics and map them to UI models on a background thread
                val (favUiList, favRegionUiList, recentUiList) = withContext(Dispatchers.Default) {
                    val batchParkIds = (favs.map { it.id } + recents.map { it.id }).distinct()
                    val batchMetricsList = repository.getMetricsForParks(batchParkIds)
                    val batchMetricsByParkId = batchMetricsList.groupBy { it.subjectId }

                    val favUiList = favs.map { park ->
                        mapToFavoriteParkUiModel(park, batchMetricsByParkId)
                    }

                    val favRegionUiList = favRegionSummaries.map { region ->
                        FavoriteRegionUiModel(
                            id = region.regionId,
                            name = region.name,
                            type = region.regionType,
                            typeLabel = when (region.regionType.lowercase()) {
                                "city" -> "Gemeinde"
                                "district" -> "Landkreis"
                                "state" -> "Bundesland"
                                else -> "Region"
                            },
                            production = formatProduction(region.annualProductionKwh),
                            co2Reduction = formatCo2(region.co2SavingsKg),
                            thumbnail = getThumbnailForId(region.regionId),
                            isFavorite = true
                        )
                    }

                    val recentUiList = recents.map { park ->
                        mapToFavoriteParkUiModel(park, batchMetricsByParkId)
                    }
                    Triple(favUiList, favRegionUiList, recentUiList)
                }

                if (requestId == loadRequestId) {
                    uiState = FavoritesUiState(
                        parks = favUiList,
                        regions = favRegionUiList,
                        recents = recentUiList,
                        isLoading = false,
                        hasLoaded = true,
                    )
                    hasLoaded = true
                }
            } catch (e: Throwable) {
                if (requestId == loadRequestId) {
                    uiState = uiState.copy(
                        isLoading = false,
                        hasLoaded = true,
                    )
                    hasLoaded = true
                }
            } finally {
                if (requestId == loadRequestId) {
                    isLoading = false
                }
            }
        }
    }

    private fun formatProduction(value: Double?): String {
        if (value == null) return "k.A."
        val gwh = value / 1_000_000.0
        return "${formatGermanNumber(gwh, 1)} GWh"
    }

    private fun formatCo2(value: Double?): String {
        if (value == null) return "k.A."
        val tons = value / 1000.0
        return "${formatGermanNumber(tons.toInt())} t"
    }

    private fun getThumbnailForId(id: String): FavoriteParkThumbnail {
        val hash = id.hashCode().coerceAtLeast(0)
        return when (hash % 8) {
            0 -> FavoriteParkThumbnail.Nordsee
            1 -> FavoriteParkThumbnail.Ostsee
            2 -> FavoriteParkThumbnail.Alpen
            3 -> FavoriteParkThumbnail.Feld
            4 -> FavoriteParkThumbnail.Waldkante
            5 -> FavoriteParkThumbnail.Herbst
            6 -> FavoriteParkThumbnail.Winter
            else -> FavoriteParkThumbnail.Dorf
        }
    }

    private fun mapToFavoriteParkUiModel(
        park: WindPark,
        metricsByParkId: Map<String, List<Metric>>
    ): FavoriteParkUiModel {
        val metrics = metricsByParkId[park.id] ?: emptyList()
        val prodMetric = metrics.firstOrNull { it.metricType == "annual_production" }
        val co2Metric = metrics.firstOrNull { it.metricType == "co2_savings" }
        return FavoriteParkUiModel(
            id = park.id,
            name = park.name,
            distance = "Gemeinde ${park.municipalityName}",
            production = formatProduction(prodMetric?.value),
            co2Reduction = formatCo2(co2Metric?.value),
            thumbnail = getThumbnailForId(park.id),
            isFavorite = park.isFavorite,
        )
    }
}

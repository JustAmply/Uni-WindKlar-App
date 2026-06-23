package app.feature.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.data.repository.WindParkRepository
import kotlinx.coroutines.launch
import app.core.model.FavoriteRegion

class FavoritesViewModel(private val repository: WindParkRepository) : ViewModel() {
    var uiState: FavoritesUiState by mutableStateOf(FavoritesUiState())
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val favs = repository.getFavoriteWindParks()
            val recents = repository.getRecentWindParks(5)
            val favRegions = repository.getFavoriteRegions()
            
            val allParks = repository.getWindParks()
            val allMetrics = repository.getAllMetrics()
            
            val favUiList = favs.map { park ->
                val metrics = repository.getMetricsForPark(park.id)
                val prodMetric = metrics.firstOrNull { it.metricType == "annual_production" }
                val co2Metric = metrics.firstOrNull { it.metricType == "co2_savings" }
                
                val prodStr = formatProduction(prodMetric?.value)
                val co2Str = formatCo2(co2Metric?.value)
                
                FavoriteParkUiModel(
                    id = park.id,
                    name = park.name,
                    distance = "Gemeinde ${park.municipalityName}",
                    production = prodStr,
                    co2Reduction = co2Str,
                    thumbnail = getThumbnailForId(park.id),
                    isFavorite = park.isFavorite,
                )
            }

            val favRegionUiList = favRegions.map { region ->
                val regionParks = allParks.filter { park ->
                    when (region.type.lowercase()) {
                        "city" -> park.municipalityId == region.id
                        "district" -> park.districtId == region.id
                        "state" -> park.stateId == region.id
                        else -> false
                    }
                }
                
                val regionParkIds = regionParks.map { it.id }.toSet()
                val regionMetrics = allMetrics.filter { it.subjectId in regionParkIds }
                
                val prodMetricSum = regionMetrics.filter { it.metricType == "annual_production" }.sumOf { it.value ?: 0.0 }
                val co2MetricSum = regionMetrics.filter { it.metricType == "co2_savings" }.sumOf { it.value ?: 0.0 }
                
                val prodStr = formatProduction(prodMetricSum)
                val co2Str = formatCo2(co2MetricSum)
                
                FavoriteRegionUiModel(
                    id = region.id,
                    name = region.name,
                    type = region.type,
                    typeLabel = when (region.type.lowercase()) {
                        "city" -> "Gemeinde"
                        "district" -> "Landkreis"
                        "state" -> "Bundesland"
                        else -> "Region"
                    },
                    production = prodStr,
                    co2Reduction = co2Str,
                    thumbnail = getThumbnailForId(region.id),
                    isFavorite = true
                )
            }

            val recentUiList = recents.map { park ->
                val metrics = repository.getMetricsForPark(park.id)
                val prodMetric = metrics.firstOrNull { it.metricType == "annual_production" }
                val co2Metric = metrics.firstOrNull { it.metricType == "co2_savings" }
                
                val prodStr = formatProduction(prodMetric?.value)
                val co2Str = formatCo2(co2Metric?.value)
                
                FavoriteParkUiModel(
                    id = park.id,
                    name = park.name,
                    distance = "Gemeinde ${park.municipalityName}",
                    production = prodStr,
                    co2Reduction = co2Str,
                    thumbnail = getThumbnailForId(park.id),
                    isFavorite = park.isFavorite,
                )
            }

            uiState = FavoritesUiState(
                parks = favUiList,
                regions = favRegionUiList,
                recents = recentUiList
            )
        }
    }

    private fun formatProduction(value: Double?): String {
        if (value == null) return "k.A."
        val gwh = value / 1_000_000.0
        return "${gwh.roundTo(1)} GWh"
    }

    private fun formatCo2(value: Double?): String {
        if (value == null) return "k.A."
        val tons = value / 1000.0
        return "${formatNumber(tons.toInt())} t"
    }

    private fun getThumbnailForId(id: String): FavoriteParkThumbnail {
        val hash = id.hashCode().coerceAtLeast(0)
        return when (hash % 3) {
            0 -> FavoriteParkThumbnail.Nordsee
            1 -> FavoriteParkThumbnail.Ostsee
            else -> FavoriteParkThumbnail.Alpen
        }
    }

    private fun Double.roundTo(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }

    private fun formatNumber(number: Int): String {
        return number.toString().reversed().chunked(3).joinToString(".").reversed()
    }
}

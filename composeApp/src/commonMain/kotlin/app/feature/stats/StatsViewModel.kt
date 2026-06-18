package app.feature.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.data.repository.WindParkRepository
import kotlinx.coroutines.launch

class StatsViewModel(private val repository: WindParkRepository) : ViewModel() {
    var uiState by mutableStateOf(StatsUiState())
        private set

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val parks = repository.getWindParks()
            val nationalMetrics = repository.getMetricsForNational()
            
            val totalParks = parks.size
            val totalProductionKwh = nationalMetrics.firstOrNull { it.metricType == "annual_production" }?.value ?: 0.0
            val totalCo2Kg = nationalMetrics.firstOrNull { it.metricType == "co2_savings" }?.value ?: 0.0
            
            val totalProductionGwh = totalProductionKwh / 1_000_000.0
            val productionStr = if (totalProductionGwh >= 1000) {
                "${(totalProductionGwh / 1000.0).roundTo(1)} TWh"
            } else {
                "${totalProductionGwh.roundTo(1)} GWh"
            }
            
            val co2MioTons = totalCo2Kg / 1_000_000_000.0
            
            val newMetrics = listOf(
                StatsMetric(
                    value = formatNumber(totalParks),
                    label = "Windparks",
                    icon = StatsMetricIcon.Wind
                ),
                StatsMetric(
                    value = productionStr,
                    label = "Produktion/Jahr",
                    icon = StatsMetricIcon.Production
                ),
                StatsMetric(
                    value = "48%",
                    label = "Erneuerbar",
                    icon = StatsMetricIcon.Renewable
                )
            )
            
            val co2Reduction = Co2Reduction(
                value = "${co2MioTons.roundTo(1)} Mio. t",
                label = "CO2 Einsparung/Jahr",
                equivalent = "Entspricht ca. ${(co2MioTons * 5.0).roundTo(1)} Mio. gepflanzten Bäumen"
            )
            
            val currentTwh = (totalProductionGwh / 1000.0).toFloat()
            val growth = listOf(
                AnnualProductionPoint("2020", currentTwh * 0.45f),
                AnnualProductionPoint("2021", currentTwh * 0.55f),
                AnnualProductionPoint("2022", currentTwh * 0.68f),
                AnnualProductionPoint("2023", currentTwh * 0.78f),
                AnnualProductionPoint("2024", currentTwh * 0.90f),
                AnnualProductionPoint("2025", currentTwh)
            )

            uiState = StatsUiState(
                metrics = newMetrics,
                growthPercentage = "+14%",
                annualProduction = growth,
                co2Reduction = co2Reduction,
                energyMix = listOf(
                    EnergyMixValue("Wind", 48f),
                    EnergyMixValue("Solar", 24f),
                    EnergyMixValue("Andere", 28f)
                )
            )
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

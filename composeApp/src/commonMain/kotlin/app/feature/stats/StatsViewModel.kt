package app.feature.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.core.model.Metric
import app.core.model.SnapshotAssumption
import app.core.model.WindPark
import app.core.model.RankingItem
import app.core.model.RankingDetailLine

import app.data.repository.WindParkRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.round
import app.core.util.formatGermanNumber
import app.core.util.roundTo

private const val DEFAULT_FULL_LOAD_HOURS = 2_000.0
private const val DEFAULT_EMISSION_FACTOR_KG_PER_KWH = 0.38
private const val DEFAULT_HOUSEHOLD_CONSUMPTION_KWH = 3_500.0
private const val DEFAULT_MUNICIPAL_BENEFIT_EUR_PER_KWH = 0.002
private const val CO2_PER_BERLIN_NYC_FLIGHT_KG = 1_000.0
private const val CO2_PER_CAR_YEAR_KG = 1_500.0
private const val CO2_PER_COAL_PLANT_YEAR_KG = 1_250_000_000.0

class StatsViewModel(private val repository: WindParkRepository) : ViewModel() {
    var uiState by mutableStateOf(StatsUiState())
        private set

    private var loadedParks: List<WindPark> = emptyList()
    private var loadedMetricsByParkId: Map<String, List<Metric>> = emptyMap()
    private var loadedCities: List<CityStat> = emptyList()
    private var loadedDistricts: List<DistrictStat> = emptyList()
    private var loadedStates: List<StateStat> = emptyList()
    private var loadedAssumptions: List<SnapshotAssumption> = emptyList()
    private val parkMetricCache = mutableMapOf<String, List<Metric>>()

    init {
        loadStats()
    }

    fun refresh() {
        loadStats()
    }

    fun setComparisonType(type: ComparisonType) {
        uiState = uiState.copy(comparisonType = type)
        updateComparison()
    }

    fun setSelectedTab(tab: StatsTab) {
        uiState = uiState.copy(selectedTab = tab)
    }

    fun setRankingType(type: RankingType) {
        uiState = uiState.copy(
            rankingType = type,
            rankingItems = buildRankingItems(type, loadedParks, loadedCities, loadedDistricts, loadedStates)
        )
    }

    fun selectInComparison(type: RankingType, itemId: String) {
        when (type) {
            RankingType.PARKS -> {
                uiState = uiState.copy(
                    comparisonType = ComparisonType.PARKS,
                    selectedParkA = uiState.allParks.firstOrNull { it.id == itemId },
                    selectedTab = StatsTab.COMPARISON
                )
            }
            RankingType.CITIES -> {
                uiState = uiState.copy(
                    comparisonType = ComparisonType.CITIES,
                    selectedCityA = uiState.allCities.firstOrNull { it.id == itemId },
                    selectedTab = StatsTab.COMPARISON
                )
            }
            RankingType.DISTRICTS -> {
                uiState = uiState.copy(
                    comparisonType = ComparisonType.DISTRICTS,
                    selectedDistrictA = uiState.allDistricts.firstOrNull { it.id == itemId },
                    selectedTab = StatsTab.COMPARISON
                )
            }
            RankingType.STATES -> {
                uiState = uiState.copy(
                    comparisonType = ComparisonType.STATES,
                    selectedStateA = uiState.allStates.firstOrNull { it.id == itemId },
                    selectedTab = StatsTab.COMPARISON
                )
            }
        }
        updateComparison()
    }

    fun selectParkA(parkId: String?) {
        uiState = uiState.copy(selectedParkA = uiState.allParks.firstOrNull { it.id == parkId })
        updateComparison()
    }

    fun selectParkB(parkId: String?) {
        uiState = uiState.copy(selectedParkB = uiState.allParks.firstOrNull { it.id == parkId })
        updateComparison()
    }

    fun selectCityA(cityId: String?) {
        uiState = uiState.copy(selectedCityA = uiState.allCities.firstOrNull { it.id == cityId })
        updateComparison()
    }

    fun selectCityB(cityId: String?) {
        uiState = uiState.copy(selectedCityB = uiState.allCities.firstOrNull { it.id == cityId })
        updateComparison()
    }

    fun selectDistrictA(districtId: String?) {
        uiState = uiState.copy(selectedDistrictA = uiState.allDistricts.firstOrNull { it.id == districtId })
        updateComparison()
    }

    fun selectDistrictB(districtId: String?) {
        uiState = uiState.copy(selectedDistrictB = uiState.allDistricts.firstOrNull { it.id == districtId })
        updateComparison()
    }

    fun selectStateA(stateId: String?) {
        uiState = uiState.copy(selectedStateA = uiState.allStates.firstOrNull { it.id == stateId })
        updateComparison()
    }

    fun selectStateB(stateId: String?) {
        uiState = uiState.copy(selectedStateB = uiState.allStates.firstOrNull { it.id == stateId })
        updateComparison()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val parks = repository.getWindParks()
            val activeTurbineCount = repository.countActiveWindTurbines()
            val nationalMetrics = repository.getMetricsForNational()
            val recentParks = repository.getRecentWindParks(limit = 1)
            val assumptions = repository.getSnapshotAssumptions()
            val attribution = repository.getSnapshotAttribution()
            val snapshotInfo = repository.getSnapshotInfo()

            val allMetrics = repository.getMetricsForParks(parks.map { it.id })
            val newState = withContext(Dispatchers.Default) {
                loadedMetricsByParkId = allMetrics.groupBy { it.subjectId }
                loadedParks = parks
                loadedAssumptions = assumptions
                parkMetricCache.clear()

                val totalCapacityKw = parks.sumOf { it.installedCapacityKw ?: 0L }
                val totalCapacityMw = totalCapacityKw / 1_000.0
                val totalProductionKwh = nationalMetrics.firstValue("annual_production")
                    ?: totalCapacityKw * assumptionValue("full_load_hours", assumptions, DEFAULT_FULL_LOAD_HOURS)
                val totalCo2Kg = nationalMetrics.firstValue("co2_savings") ?: 0.0
                val totalHouseholds = nationalMetrics.firstValue("household_equivalent") ?: 0.0
                val totalMunicipalBenefit = nationalMetrics.firstValue("municipal_participation") ?: 0.0

                val cities = buildCityStats(parks, totalCapacityMw, assumptions, loadedMetricsByParkId)
                loadedCities = cities

                val districts = buildDistrictStats(parks, totalCapacityMw, assumptions, loadedMetricsByParkId)
                loadedDistricts = districts

                val states = buildStateStats(parks, totalCapacityMw, assumptions, loadedMetricsByParkId)
                loadedStates = states

                val recentPark = recentParks.firstOrNull()
                val selectedDistrict = selectDistrictComparison(
                    districts = districts,
                    recentPark = recentPark,
                )

                val parkOptions = parks
                    .sortedWith(compareByDescending<WindPark> { it.installedCapacityKw ?: 0L }.thenBy { it.name })
                    .map { it.toComparisonOption() }
                val cityOptions = cities.map { it.toComparisonOption() }
                val districtOptions = districts.map { it.toComparisonOption() }
                val stateOptions = states.map { it.toComparisonOption() }

                val selectedParkA = recentPark?.let { recent ->
                    parkOptions.firstOrNull { it.id == recent.id }
                } ?: parkOptions.firstOrNull()
                val selectedParkB = parkOptions.firstOrNull { it.id != selectedParkA?.id }

                val selectedCityA = recentPark?.let { recent ->
                    cityOptions.firstOrNull { it.id == recent.municipalityId }
                } ?: cityOptions.firstOrNull()
                val selectedCityB = cityOptions.firstOrNull { it.id != selectedCityA?.id }

                val selectedDistrictA = recentPark?.let { recent ->
                    districtOptions.firstOrNull { it.id == recent.districtId }
                } ?: districtOptions.firstOrNull()
                val selectedDistrictB = districtOptions.firstOrNull { it.id != selectedDistrictA?.id }

                val selectedStateA = recentPark?.let { recent ->
                    stateOptions.firstOrNull { it.id == recent.stateId }
                } ?: stateOptions.firstOrNull()
                val selectedStateB = stateOptions.firstOrNull { it.id != selectedStateA?.id }

                val comparisonRows = buildParkComparisonRows(
                    parkIdA = selectedParkA?.id,
                    parkIdB = selectedParkB?.id,
                    assumptions = assumptions,
                )

                StatsUiState(
                    subtitle = snapshotInfo?.let { "Deutschland · Snapshot ${formatGermanDate(it.mastrExportDate)}" }
                        ?: "Deutschland · Snapshot",
                    snapshotInfoLine = "Lokaler Datenstand · keine Live-Daten",
                    overviewCards = listOf(
                        StatsOverviewCard(
                            value = formatInteger(parks.size),
                            label = "Windparks",
                            icon = StatsIcon.Wind,
                        ),
                        StatsOverviewCard(
                            value = formatEnergy(totalProductionKwh),
                            label = "Produktion",
                            icon = StatsIcon.Production,
                        ),
                        StatsOverviewCard(
                            value = formatCapacity(totalCapacityMw),
                            label = "Leistung",
                            icon = StatsIcon.Capacity,
                        ),
                    ),
                    impactCards = listOf(
                        StatsImpactCard(
                            title = "Haushalte",
                            value = formatCompact(totalHouseholds),
                            description = "rechnerisch mit Windstrom versorgt",
                            quality = "estimated",
                            icon = StatsIcon.Household,
                        ),
                        StatsImpactCard(
                            title = "Kommunaler Nutzen",
                            value = formatCurrency(totalMunicipalBenefit),
                            description = "mögliche Beteiligung für Windenergie an Land nach § 6 EEG",
                            quality = "estimated",
                            icon = StatsIcon.Money,
                        ),
                        StatsImpactCard(
                            title = "Anlagen",
                            value = formatInteger(activeTurbineCount),
                            description = "MaStR/Open-MaStR-Stammdaten im Snapshot",
                            quality = "official",
                            icon = StatsIcon.Wind,
                        ),
                        StatsImpactCard(
                            title = "CO2 gespart",
                            value = formatCo2(totalCo2Kg),
                            description = "vermiedene Emissionen pro Jahr",
                            quality = "estimated",
                            icon = StatsIcon.Co2,
                        ),
                    ),
                    rankingType = RankingType.DISTRICTS,
                    rankingItems = buildRankingItems(
                        RankingType.DISTRICTS,
                        parks,
                        cities,
                        districts,
                        states,
                    ),
                    districtComparison = selectedDistrict,
                    comparisonType = ComparisonType.PARKS,
                    allParks = parkOptions,
                    allCities = cityOptions,
                    allDistricts = districtOptions,
                    allStates = stateOptions,
                    selectedParkA = selectedParkA,
                    selectedParkB = selectedParkB,
                    selectedCityA = selectedCityA,
                    selectedCityB = selectedCityB,
                    selectedDistrictA = selectedDistrictA,
                    selectedDistrictB = selectedDistrictB,
                    selectedStateA = selectedStateA,
                    selectedStateB = selectedStateB,
                    comparisonRows = comparisonRows,
                    co2Summary = formatCo2(totalCo2Kg),
                    co2Comparisons = buildCo2Comparisons(totalCo2Kg),
                    capacityClasses = buildCapacityClasses(parks),
                    qualityNotes = listOf(
                        StatsQualityNote(
                            label = "Windanlagen",
                            quality = "official",
                            description = "Stammdaten aus MaStR/Open-MaStR.",
                        ),
                        StatsQualityNote(
                            label = "Windparks",
                            quality = "derived",
                            description = "Gruppierung wird in der Vorverarbeitung aus Anlagendaten gebildet.",
                        ),
                        StatsQualityNote(
                            label = "Wirkungswerte",
                            quality = "estimated",
                            description = "Produktion, CO2 und kommunaler Nutzen für Windenergie an Land beruhen auf dokumentierten MVP-Annahmen.",
                        ),
                    ),
                    attribution = attribution,
                    isLoading = false,
                )
            }
            uiState = newState.copy(selectedTab = uiState.selectedTab)
        }
    }

    private fun updateComparison() {
        viewModelScope.launch {
            val currentState = uiState
            val rows = withContext(Dispatchers.Default) {
                when (currentState.comparisonType) {
                    ComparisonType.PARKS -> buildParkComparisonRows(
                        parkIdA = currentState.selectedParkA?.id,
                        parkIdB = currentState.selectedParkB?.id,
                        assumptions = loadedAssumptions,
                    )
                    ComparisonType.CITIES -> buildCityComparisonRows(
                        cityIdA = currentState.selectedCityA?.id,
                        cityIdB = currentState.selectedCityB?.id,
                    )
                    ComparisonType.DISTRICTS -> buildDistrictComparisonRows(
                        districtIdA = currentState.selectedDistrictA?.id,
                        districtIdB = currentState.selectedDistrictB?.id,
                    )
                    ComparisonType.STATES -> buildStateComparisonRows(
                        stateIdA = currentState.selectedStateA?.id,
                        stateIdB = currentState.selectedStateB?.id,
                    )
                }
            }
            uiState = uiState.copy(comparisonRows = rows)
        }
    }

    private fun buildCityStats(
        parks: List<WindPark>,
        totalCapacityMw: Double,
        assumptions: List<SnapshotAssumption>,
        metricsByParkId: Map<String, List<Metric>>,
    ): List<CityStat> {
        val parkGroups = parks
            .groupBy { it.municipalityId }
        val stateCapacityMw = parks
            .groupBy { it.stateId }
            .mapValues { (_, stateParks) ->
                stateParks.sumOf { it.installedCapacityKw ?: 0L } / 1_000.0
            }

        return parkGroups.map { (cityId, cityParks) ->
            val capacityMw = cityParks.sumOf { it.installedCapacityKw ?: 0L } / 1_000.0
            val turbines = cityParks.sumOf { it.turbineCount }
            val firstPark = cityParks.first()
            val stateCapacity = stateCapacityMw[firstPark.stateId] ?: 0.0
            val municipalBenefitEur = cityParks
                .takeIf { it.isNotEmpty() }
                ?.sumOf { park ->
                    metricsByParkId[park.id]
                        ?.firstOrNull { it.metricType == "municipal_participation" }
                        ?.value
                        ?: 0.0
                }
            CityStat(
                cityId = cityId,
                label = firstPark.municipalityName,
                districtName = firstPark.districtName,
                stateName = firstPark.stateName,
                windParkCount = cityParks.size,
                turbineCount = turbines,
                installedCapacityMw = capacityMw,
                shareOfNationalCapacity = if (totalCapacityMw > 0.0) {
                    (capacityMw / totalCapacityMw).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                },
                shareOfStateCapacity = if (stateCapacity > 0.0) {
                    (capacityMw / stateCapacity).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                },
                municipalBenefitEur = municipalBenefitEur,
            )
        }.sortedByDescending { it.installedCapacityMw }
    }

    private fun buildDistrictStats(
        parks: List<WindPark>,
        totalCapacityMw: Double,
        assumptions: List<SnapshotAssumption>,
        metricsByParkId: Map<String, List<Metric>>,
    ): List<DistrictStat> {
        val parkGroups = parks
            .groupBy { it.districtId }
        val stateCapacityMw = parks
            .groupBy { it.stateId }
            .mapValues { (_, stateParks) ->
                stateParks.sumOf { it.installedCapacityKw ?: 0L } / 1_000.0
            }

        return parkGroups.map { (districtId, districtParks) ->
            val capacityMw = districtParks.sumOf { it.installedCapacityKw ?: 0L } / 1_000.0
            val turbines = districtParks.sumOf { it.turbineCount }
            val firstPark = districtParks.first()
            val representativeMunicipality = districtParks
                .groupBy { it.municipalityName }
                .maxByOrNull { (_, municipalityParks) ->
                    municipalityParks.sumOf { it.installedCapacityKw ?: 0L }
                }
                ?.key
                ?: firstPark.municipalityName
            val stateCapacity = stateCapacityMw[firstPark.stateId] ?: 0.0
            val municipalBenefitEur = districtParks
                .takeIf { it.isNotEmpty() }
                ?.sumOf { park ->
                    metricsByParkId[park.id]
                        ?.firstOrNull { it.metricType == "municipal_participation" }
                        ?.value
                        ?: 0.0
                }
            DistrictStat(
                districtId = districtId,
                label = firstPark.districtName,
                contextLabel = representativeMunicipality,
                stateName = firstPark.stateName,
                windParkCount = districtParks.size,
                turbineCount = turbines,
                installedCapacityMw = capacityMw,
                shareOfNationalCapacity = if (totalCapacityMw > 0.0) {
                    (capacityMw / totalCapacityMw).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                },
                shareOfStateCapacity = if (stateCapacity > 0.0) {
                    (capacityMw / stateCapacity).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                },
                municipalBenefitEur = municipalBenefitEur,
            )
        }.sortedByDescending { it.installedCapacityMw }
    }

    private fun buildStateStats(
        parks: List<WindPark>,
        totalCapacityMw: Double,
        assumptions: List<SnapshotAssumption>,
        metricsByParkId: Map<String, List<Metric>>,
    ): List<StateStat> {
        val parkGroups = parks
            .groupBy { it.stateId }

        return parkGroups.map { (stateId, stateParks) ->
            val capacityMw = stateParks.sumOf { it.installedCapacityKw ?: 0L } / 1_000.0
            val turbines = stateParks.sumOf { it.turbineCount }
            val firstPark = stateParks.first()
            val municipalBenefitEur = stateParks
                .takeIf { it.isNotEmpty() }
                ?.sumOf { park ->
                    metricsByParkId[park.id]
                        ?.firstOrNull { it.metricType == "municipal_participation" }
                        ?.value
                        ?: 0.0
                }
            StateStat(
                stateId = stateId,
                label = firstPark.stateName,
                windParkCount = stateParks.size,
                turbineCount = turbines,
                installedCapacityMw = capacityMw,
                shareOfNationalCapacity = if (totalCapacityMw > 0.0) {
                    (capacityMw / totalCapacityMw).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                },
                municipalBenefitEur = municipalBenefitEur,
            )
        }.sortedByDescending { it.installedCapacityMw }
    }

    private fun selectDistrictComparison(
        districts: List<DistrictStat>,
        recentPark: WindPark?,
    ): DistrictComparison? {
        if (districts.isEmpty()) return null

        val district = recentPark
            ?.districtId
            ?.let { districtId -> districts.firstOrNull { it.districtId == districtId } }
            ?: districts.first()

        val rank = districts.indexOfFirst { it.districtId == district.districtId } + 1
        val contextLabel = recentPark?.let { "Zuletzt geöffnet: Gemeinde ${it.municipalityName}" }
            ?: "Kein zuletzt geöffneter Park"

        return DistrictComparison(
            label = district.label,
            contextLabel = contextLabel,
            rankText = "Rang $rank von ${districts.size}",
            installedCapacity = formatCapacity(district.installedCapacityMw),
            windParks = formatInteger(district.windParkCount),
            turbines = formatInteger(district.turbineCount),
            nationalShare = formatPercent(district.shareOfNationalCapacity),
            shareProgress = district.shareOfNationalCapacity,
            isFallback = recentPark == null,
        )
    }

    private suspend fun buildParkComparisonRows(
        parkIdA: String?,
        parkIdB: String?,
        assumptions: List<SnapshotAssumption>,
    ): List<ComparisonRow> {
        val parkA = loadedParks.firstOrNull { it.id == parkIdA }
        val parkB = loadedParks.firstOrNull { it.id == parkIdB }
        if (parkA == null || parkB == null) return emptyList()

        return buildComparisonRows(
            metricsA = parkA.comparisonMetrics(metricsForPark(parkA.id), assumptions),
            metricsB = parkB.comparisonMetrics(metricsForPark(parkB.id), assumptions),
        )
    }

    private fun buildCityComparisonRows(
        cityIdA: String?,
        cityIdB: String?,
    ): List<ComparisonRow> {
        val cityA = loadedCities.firstOrNull { it.cityId == cityIdA }
        val cityB = loadedCities.firstOrNull { it.cityId == cityIdB }
        if (cityA == null || cityB == null) return emptyList()

        return buildComparisonRows(
            metricsA = cityA.comparisonMetrics(),
            metricsB = cityB.comparisonMetrics(),
        )
    }

    private fun buildDistrictComparisonRows(
        districtIdA: String?,
        districtIdB: String?,
    ): List<ComparisonRow> {
        val districtA = loadedDistricts.firstOrNull { it.districtId == districtIdA }
        val districtB = loadedDistricts.firstOrNull { it.districtId == districtIdB }
        if (districtA == null || districtB == null) return emptyList()

        return buildComparisonRows(
            metricsA = districtA.comparisonMetrics(),
            metricsB = districtB.comparisonMetrics(),
        )
    }

    private fun buildStateComparisonRows(
        stateIdA: String?,
        stateIdB: String?,
    ): List<ComparisonRow> {
        val stateA = loadedStates.firstOrNull { it.stateId == stateIdA }
        val stateB = loadedStates.firstOrNull { it.stateId == stateIdB }
        if (stateA == null || stateB == null) return emptyList()

        return buildComparisonRows(
            metricsA = stateA.comparisonMetrics(),
            metricsB = stateB.comparisonMetrics(),
        )
    }

    private fun buildComparisonRows(
        metricsA: ComparisonMetrics,
        metricsB: ComparisonMetrics,
    ): List<ComparisonRow> = buildList {
        add(comparisonRow("Anzahl Anlagen", metricsA.turbines.toDouble(), metricsB.turbines.toDouble()) {
            formatInteger(it.roundToInt())
        }!!)
        add(comparisonRow("Leistung", metricsA.capacityMw, metricsB.capacityMw) {
            "${formatGermanNumber(it, 1)} MW"
        }!!)
        add(comparisonRow("Jahresproduktion", metricsA.annualProductionKwh, metricsB.annualProductionKwh) {
            "${formatGermanNumber(it / 1_000_000.0, 1)} GWh"
        }!!)
        add(comparisonRow("CO2-Einsparung", metricsA.co2Kg, metricsB.co2Kg) {
            "${formatGermanNumber(it / 1_000.0, 0)} t"
        }!!)
        add(comparisonRow("Haushaltsäquivalente", metricsA.households, metricsB.households) {
            formatCompact(it)
        }!!)
        comparisonRow("Kommunale Beteiligung an Land", metricsA.municipalBenefit, metricsB.municipalBenefit) {
            formatCurrency(it)
        }?.let { add(it) }
    }

    private suspend fun metricsForPark(parkId: String): List<Metric> {
        parkMetricCache[parkId]?.let { return it }
        return repository.getMetricsForPark(parkId).also { parkMetricCache[parkId] = it }
    }

    private fun WindPark.comparisonMetrics(
        metrics: List<Metric>,
        assumptions: List<SnapshotAssumption>,
    ): ComparisonMetrics {
        val capacityKw = installedCapacityKw?.toDouble() ?: 0.0
        val annualProduction = metrics.firstValue("annual_production")
            ?: capacityKw * assumptionValue("full_load_hours", assumptions, DEFAULT_FULL_LOAD_HOURS)
        val co2 = metrics.firstValue("co2_savings")
            ?: annualProduction * assumptionValue(
                "emission_factor_kg_per_kwh",
                assumptions,
                DEFAULT_EMISSION_FACTOR_KG_PER_KWH,
            )
        val households = metrics.firstValue("household_equivalent")
            ?: metrics.firstValue("households_supplied")
            ?: annualProduction / assumptionValue(
                "household_consumption_kwh",
                assumptions,
                DEFAULT_HOUSEHOLD_CONSUMPTION_KWH,
            )
        val municipalBenefit = metrics.firstValue("municipal_participation")
            ?: annualProduction * assumptionValue(
                "municipal_benefit_eur_per_kwh",
                assumptions,
                DEFAULT_MUNICIPAL_BENEFIT_EUR_PER_KWH,
            )
        return ComparisonMetrics(
            turbines = turbineCount,
            capacityMw = capacityKw / 1_000.0,
            annualProductionKwh = annualProduction,
            co2Kg = co2,
            households = households,
            municipalBenefit = municipalBenefit,
        )
    }

    private fun aggregateComparisonMetrics(
        parks: List<WindPark>,
        capacityMw: Double,
        turbineCount: Int,
        municipalBenefit: Double?,
    ): ComparisonMetrics {
        val parkIds = parks.map { it.id }.toSet()
        val regionMetrics = loadedMetricsByParkId.filterKeys { it in parkIds }.values.flatten()
        val annualProduction = regionMetrics.filter { it.metricType == "annual_production" }.sumOf { it.value ?: 0.0 }
        val co2 = regionMetrics.filter { it.metricType == "co2_savings" }.sumOf { it.value ?: 0.0 }
        val households = regionMetrics.filter { it.metricType == "household_equivalent" || it.metricType == "households_supplied" }.sumOf { it.value ?: 0.0 }

        return ComparisonMetrics(
            turbines = turbineCount,
            capacityMw = capacityMw,
            annualProductionKwh = annualProduction,
            co2Kg = co2,
            households = households,
            municipalBenefit = municipalBenefit,
        )
    }

    private fun CityStat.comparisonMetrics(): ComparisonMetrics {
        val cityParks = loadedParks.filter { it.municipalityId == cityId }
        return aggregateComparisonMetrics(cityParks, installedCapacityMw, turbineCount, municipalBenefitEur)
    }

    private fun DistrictStat.comparisonMetrics(): ComparisonMetrics {
        val districtParks = loadedParks.filter { it.districtId == districtId }
        return aggregateComparisonMetrics(districtParks, installedCapacityMw, turbineCount, municipalBenefitEur)
    }

    private fun StateStat.comparisonMetrics(): ComparisonMetrics {
        val stateParks = loadedParks.filter { it.stateId == stateId }
        return aggregateComparisonMetrics(stateParks, installedCapacityMw, turbineCount, municipalBenefitEur)
    }

    private fun comparisonRow(
        label: String,
        rawA: Double?,
        rawB: Double?,
        formatter: (Double) -> String,
    ): ComparisonRow? {
        if (rawA == null && rawB == null) return null
        val comparableA = rawA ?: 0.0
        val comparableB = rawB ?: 0.0
        val max = maxOf(comparableA, comparableB).takeIf { it > 0.0 } ?: 1.0
        return ComparisonRow(
            label = label,
            valueA = rawA?.let(formatter) ?: "Nicht anwendbar",
            valueB = rawB?.let(formatter) ?: "Nicht anwendbar",
            ratioA = (comparableA / max).toFloat().coerceIn(0f, 1f),
            ratioB = (comparableB / max).toFloat().coerceIn(0f, 1f),
            isHigherA = when {
                rawA != null && rawB != null -> rawA >= rawB
                rawA != null -> true
                else -> false
            },
        )
    }

    private fun buildCo2Comparisons(totalCo2Kg: Double): List<Co2Comparison> {
        val values = listOf(
            "Flüge Berlin-NYC" to totalCo2Kg / CO2_PER_BERLIN_NYC_FLIGHT_KG,
            "Auto-Jahresfahrten" to totalCo2Kg / CO2_PER_CAR_YEAR_KG,
            "Kohlekraftwerksjahre" to totalCo2Kg / CO2_PER_COAL_PLANT_YEAR_KG,
        )
        return values.map { (label, value) ->
            Co2Comparison(
                label = label,
                value = when (label) {
                    "Kohlekraftwerksjahre" -> "ca. ${formatGermanNumber(value, 0)} Jahre"
                    else -> "ca. ${formatCompact(value)}"
                },
                description = when (label) {
                    "Flüge Berlin-NYC" -> "als grobe Flug-Emissionseinordnung"
                    "Auto-Jahresfahrten" -> "auf Basis typischer Jahresfahrten"
                    else -> "bezogen auf ein großes Kohlekraftwerk"
                },
            )
        }
    }

    private fun buildCapacityClasses(parks: List<WindPark>): List<CapacityClassStat> {
        val classes = listOf(
            "< 5 MW" to parks.count { (it.installedCapacityKw ?: 0L) < 5_000L },
            "5-20 MW" to parks.count { (it.installedCapacityKw ?: 0L) in 5_000L until 20_000L },
            "20-50 MW" to parks.count { (it.installedCapacityKw ?: 0L) in 20_000L until 50_000L },
            "> 50 MW" to parks.count { (it.installedCapacityKw ?: 0L) >= 50_000L },
        )
        val totalCount = parks.size.coerceAtLeast(1)
        val maxCount = classes.maxOfOrNull { it.second } ?: 0
        return classes.map { (label, count) ->
            CapacityClassStat(
                label = label,
                count = count,
                share = if (maxCount > 0) count.toFloat() / maxCount else 0f,
                percentOfTotal = count.toFloat() / totalCount,
            )
        }
    }

    private fun WindPark.toComparisonOption(): ComparisonOption =
        ComparisonOption(
            id = id,
            label = name,
            description = "${municipalityName} · ${formatCapacity((installedCapacityKw ?: 0L) / 1_000.0)}",
        )

    private fun CityStat.toComparisonOption(): ComparisonOption =
        ComparisonOption(
            id = cityId,
            label = label,
            description = "$districtName, $stateName · ${formatCapacity(installedCapacityMw)} · ${formatInteger(windParkCount)} Parks",
        )

    private fun DistrictStat.toComparisonOption(): ComparisonOption =
        ComparisonOption(
            id = districtId,
            label = label,
            description = "$stateName · ${formatCapacity(installedCapacityMw)} · ${formatInteger(windParkCount)} Parks",
        )

    private fun StateStat.toComparisonOption(): ComparisonOption =
        ComparisonOption(
            id = stateId,
            label = label,
            description = "${formatCapacity(installedCapacityMw)} · ${formatInteger(windParkCount)} Parks",
        )

    private fun List<Metric>.firstValue(metricType: String): Double? =
        firstOrNull { it.metricType == metricType }?.value

    private fun assumptionValue(
        id: String,
        assumptions: List<SnapshotAssumption>,
        fallback: Double,
    ): Double = assumptions.firstOrNull { it.id == id }?.value ?: fallback

    private fun formatInteger(value: Int): String = formatGermanNumber(value)

    private fun formatGermanDate(isoDate: String): String {
        val parts = isoDate.split("-")
        if (parts.size != 3) return isoDate
        val (year, month, day) = parts
        return "$day.$month.$year"
    }

    private fun formatEnergy(kwh: Double): String {
        val twh = kwh / 1_000_000_000.0
        return if (twh >= 1.0) {
            "${formatGermanNumber(twh, 1)} TWh"
        } else {
            "${formatGermanNumber(kwh / 1_000_000.0, 1)} GWh"
        }
    }

    private fun formatCapacity(mw: Double): String =
        if (mw >= 1_000.0) {
            "${formatGermanNumber(mw / 1_000.0, 1)} GW"
        } else {
            "${formatGermanNumber(mw, 0)} MW"
        }

    private fun formatCo2(kg: Double): String {
        val mioTons = kg / 1_000_000_000.0
        return "${formatGermanNumber(mioTons, 1)} Mio. t"
    }

    private fun formatCurrency(value: Double): String =
        if (value >= 1_000_000_000.0) {
            "${formatGermanNumber(value / 1_000_000_000.0, 1)} Mrd. EUR"
        } else if (value >= 1_000_000.0) {
            "${formatGermanNumber(value / 1_000_000.0, 1)} Mio. EUR"
        } else {
            "${formatGermanNumber(value, 0)} EUR"
        }

    private fun formatCompact(value: Double): String =
        if (value >= 1_000_000.0) {
            "${formatGermanNumber(value / 1_000_000.0, 1)} Mio."
        } else {
            formatGermanNumber(round(value).toInt())
        }

    private fun formatPercent(value: Float): String =
        "${formatGermanNumber(value * 100.0, 1)} %"

    private fun Double.roundToInt(): Int = round(this).toInt()

    private fun buildRankingItems(
        type: RankingType,
        parks: List<WindPark>,
        cities: List<CityStat>,
        districts: List<DistrictStat>,
        states: List<StateStat>,
    ): List<RankingItem> {
        return when (type) {
            RankingType.PARKS -> {
                val sortedParks = parks.sortedWith(
                    compareByDescending<WindPark> { it.installedCapacityKw ?: 0L }
                        .thenBy { it.name }
                )
                val maxCapacity = sortedParks.firstOrNull()?.installedCapacityKw?.toDouble()?.coerceAtLeast(1.0) ?: 1.0
                sortedParks.mapIndexed { index, park ->
                    val capacityMw = (park.installedCapacityKw ?: 0L) / 1_000.0
                    RankingItem(
                        id = park.id,
                        rank = index + 1,
                        name = park.name,
                        subtitle = "${park.municipalityName}, ${park.stateName}",
                        valueLabel = "${formatGermanNumber(capacityMw, 1)} MW",
                        progress = ((park.installedCapacityKw ?: 0L) / maxCapacity).toFloat().coerceIn(0f, 1f),
                        details = listOf(
                            RankingDetailLine("Anlagen", formatGermanNumber(park.turbineCount)),
                            RankingDetailLine("Leistung", "${formatGermanNumber(capacityMw, 1)} MW"),
                            RankingDetailLine("Gemeinde", park.municipalityName),
                            RankingDetailLine("Datenqualität", formatDataQualityLabel(park.dataQuality)),
                        )
                    )
                }
            }
            RankingType.CITIES -> {
                val maxCapacity = cities.firstOrNull()?.installedCapacityMw?.coerceAtLeast(1.0) ?: 1.0
                cities.mapIndexed { index, city ->
                    RankingItem(
                        id = city.cityId,
                        rank = index + 1,
                        name = city.label,
                        subtitle = "${city.districtName}, ${city.stateName}",
                        valueLabel = "${formatGermanNumber(city.installedCapacityMw, 1)} MW",
                        progress = (city.installedCapacityMw / maxCapacity).toFloat().coerceIn(0f, 1f),
                        details = listOf(
                            RankingDetailLine("Windparks", formatGermanNumber(city.windParkCount)),
                            RankingDetailLine("Anlagen", formatGermanNumber(city.turbineCount)),
                            RankingDetailLine("Anteil am Bundesland", formatPercent(city.shareOfStateCapacity)),
                        )
                    )
                }
            }
            RankingType.DISTRICTS -> {
                val maxCapacity = districts.firstOrNull()?.installedCapacityMw?.coerceAtLeast(1.0) ?: 1.0
                districts.mapIndexed { index, district ->
                    RankingItem(
                        id = district.districtId,
                        rank = index + 1,
                        name = district.label,
                        subtitle = district.stateName,
                        valueLabel = "${formatGermanNumber(district.installedCapacityMw, 1)} MW",
                        progress = (district.installedCapacityMw / maxCapacity).toFloat().coerceIn(0f, 1f),
                        details = listOf(
                            RankingDetailLine("Windparks", formatGermanNumber(district.windParkCount)),
                            RankingDetailLine("Anlagen", formatGermanNumber(district.turbineCount)),
                            RankingDetailLine("Anteil am Bundesland", formatPercent(district.shareOfStateCapacity)),
                        )
                    )
                }
            }
            RankingType.STATES -> {
                val maxCapacity = states.firstOrNull()?.installedCapacityMw?.coerceAtLeast(1.0) ?: 1.0
                states.mapIndexed { index, state ->
                    RankingItem(
                        id = state.stateId,
                        rank = index + 1,
                        name = state.label,
                        subtitle = "Deutschland",
                        valueLabel = "${formatGermanNumber(state.installedCapacityMw, 1)} MW",
                        progress = (state.installedCapacityMw / maxCapacity).toFloat().coerceIn(0f, 1f),
                        details = listOf(
                            RankingDetailLine("Windparks", formatGermanNumber(state.windParkCount)),
                            RankingDetailLine("Anlagen", formatGermanNumber(state.turbineCount)),
                            RankingDetailLine("Anteil am Bund", formatPercent(state.shareOfNationalCapacity)),
                        )
                    )
                }
            }
        }
    }

    private fun formatDataQualityLabel(quality: String): String = when (quality.lowercase()) {
        "official" -> "Offiziell"
        "measured" -> "Gemessen"
        "derived" -> "Abgeleitet"
        "estimated" -> "Geschätzt"
        "simulated" -> "Simuliert"
        "missing" -> "Fehlend"
        else -> quality
    }
}

private data class ComparisonMetrics(
    val turbines: Int,
    val capacityMw: Double,
    val annualProductionKwh: Double,
    val co2Kg: Double,
    val households: Double,
    val municipalBenefit: Double?,
)

package app.feature.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.core.model.Metric
import app.core.model.NationalStatsSummary
import app.core.model.RegionSummary
import app.core.model.SnapshotAssumption
import app.core.model.WindPark
import app.core.model.RankingItem
import app.core.model.RankingDetailLine

import app.core.ui.components.formatDataQuality
import app.data.repository.WindParkRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.round
import app.core.util.formatGermanNumber
import kotlinx.coroutines.Job

class StatsViewModel(private val repository: WindParkRepository) : ViewModel() {
    var uiState by mutableStateOf(StatsUiState())
        private set

    private var hasLoaded = false
    private var loadJob: Job? = null
    private val loadingImpactDetails = mutableSetOf<String>()
    private var loadedParks: List<WindPark> = emptyList()
    private var loadedNationalSummary: NationalStatsSummary? = null
    private var loadedCitySummariesById: Map<String, RegionSummary> = emptyMap()
    private var loadedDistrictSummariesById: Map<String, RegionSummary> = emptyMap()
    private var loadedStateSummariesById: Map<String, RegionSummary> = emptyMap()
    private var loadedCities: List<CityStat> = emptyList()
    private var loadedDistricts: List<DistrictStat> = emptyList()
    private var loadedStates: List<StateStat> = emptyList()
    private var loadedAssumptions: List<SnapshotAssumption> = emptyList()
    private var totalHouseholdsValue: Double = 0.0
    private var totalCo2KgValue: Double = 0.0
    private val parkMetricCache = mutableMapOf<String, List<Metric>>()

    fun loadIfNeeded() {
        if (!hasLoaded) {
            loadStats(force = false)
        }
    }

    fun refresh() {
        loadStats(force = true)
    }

    fun loadImpactDetail(metricType: String) {
        if (!hasLoaded || uiState.isLoading || metricType in loadingImpactDetails) return
        val alreadyLoaded = when (metricType) {
            StatsImpactType.Households.name -> uiState.householdsDetail != null
            StatsImpactType.MunicipalBenefit.name -> uiState.municipalBenefitDetail != null
            StatsImpactType.Turbines.name -> uiState.turbinesDetail != null
            StatsImpactType.Co2.name -> uiState.co2Detail != null
            else -> true
        }
        if (alreadyLoaded) return

        loadingImpactDetails += metricType
        viewModelScope.launch {
            try {
                when (metricType) {
                    StatsImpactType.Households.name -> {
                        val detail = withContext(Dispatchers.Default) {
                            buildHouseholdsImpactDetailFromSummary(
                                parks = loadedParks,
                                nationalSummary = loadedNationalSummary,
                                assumptions = loadedAssumptions,
                            )
                        }
                        uiState = uiState.copy(householdsDetail = detail)
                    }
                    StatsImpactType.MunicipalBenefit.name -> {
                        val detail = withContext(Dispatchers.Default) {
                            buildMunicipalBenefitImpactDetailFromSummary(
                                parks = loadedParks,
                                districts = loadedDistricts,
                                states = loadedStates,
                                assumptions = loadedAssumptions,
                            )
                        }
                        uiState = uiState.copy(municipalBenefitDetail = detail)
                    }
                    StatsImpactType.Turbines.name -> {
                        val detail = withContext(Dispatchers.Default) {
                            buildTurbinesImpactDetailFromSummary(
                                parks = loadedParks,
                                nationalSummary = loadedNationalSummary,
                            )
                        }
                        uiState = uiState.copy(turbinesDetail = detail)
                    }
                    StatsImpactType.Co2.name -> {
                        val detail = withContext(Dispatchers.Default) {
                            buildCo2ImpactDetailFromSummary(
                                parks = loadedParks,
                                nationalSummary = loadedNationalSummary,
                                assumptions = loadedAssumptions,
                            )
                        }
                        uiState = uiState.copy(co2Detail = detail)
                    }
                }
            } finally {
                loadingImpactDetails -= metricType
            }
        }
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

    private fun loadStats(force: Boolean) {
        if (loadJob?.isActive == true) return
        if (!force && hasLoaded) return

        uiState = uiState.copy(isLoading = true)
        loadJob = viewModelScope.launch {
            val parks = repository.getWindParks()
            val nationalSummary = repository.getNationalStatsSummary()
            val citySummaries = repository.getRegionSummaries("city")
            val districtSummaries = repository.getRegionSummaries("district")
            val stateSummaries = repository.getRegionSummaries("state")
            val recentParks = repository.getRecentWindParks(limit = 1)
            val assumptions = repository.getSnapshotAssumptions()
            val attribution = repository.getSnapshotAttribution()
            val snapshotInfo = repository.getSnapshotInfo()

            val newState = withContext(Dispatchers.Default) {
                loadedParks = parks
                loadedNationalSummary = nationalSummary
                loadedCitySummariesById = citySummaries.associateBy { it.regionId }
                loadedDistrictSummariesById = districtSummaries.associateBy { it.regionId }
                loadedStateSummariesById = stateSummaries.associateBy { it.regionId }
                loadedAssumptions = assumptions
                parkMetricCache.clear()

                val totalCapacityKw = nationalSummary?.installedCapacityKw
                    ?: parks.sumOf { it.installedCapacityKw ?: 0L }
                val totalCapacityMw = totalCapacityKw / 1_000.0
                val totalProductionKwh = nationalSummary?.annualProductionKwh
                    ?: totalCapacityKw * assumptionValue("full_load_hours", assumptions, DEFAULT_FULL_LOAD_HOURS)
                val totalCo2Kg = nationalSummary?.co2SavingsKg ?: 0.0
                val totalHouseholds = nationalSummary?.householdEquivalent ?: 0.0
                val totalMunicipalBenefit = nationalSummary?.municipalBenefitEur ?: 0.0
                val activeTurbineCount = nationalSummary?.activeTurbineCount ?: parks.sumOf { it.turbineCount }
                totalHouseholdsValue = totalHouseholds
                totalCo2KgValue = totalCo2Kg

                val cities = buildCityStats(citySummaries, stateSummaries, totalCapacityMw)
                loadedCities = cities

                val districts = buildDistrictStats(districtSummaries, stateSummaries, totalCapacityMw)
                loadedDistricts = districts

                val states = buildStateStats(stateSummaries, totalCapacityMw)
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
                            value = formatInteger(nationalSummary?.windParkCount ?: parks.size),
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
                            type = StatsImpactType.Households,
                            title = "Haushalte",
                            value = formatCompact(totalHouseholds),
                            description = "rechnerisch mit Windstrom versorgt",
                            quality = "estimated",
                            metaLabel = "Geschätzt",
                            icon = StatsIcon.Household,
                        ),
                        StatsImpactCard(
                            type = StatsImpactType.MunicipalBenefit,
                            title = "Kommunaler Nutzen",
                            value = formatCurrency(totalMunicipalBenefit),
                            description = "mögliche Beteiligung für Windenergie an Land nach § 6 EEG",
                            quality = "estimated",
                            metaLabel = "Szenario",
                            icon = StatsIcon.Money,
                        ),
                        StatsImpactCard(
                            type = StatsImpactType.Turbines,
                            title = "Anlagen",
                            value = formatInteger(activeTurbineCount),
                            description = "MaStR/Open-MaStR-Stammdaten im Snapshot",
                            quality = "official",
                            metaLabel = "Offiziell",
                            icon = StatsIcon.Wind,
                        ),
                        StatsImpactCard(
                            type = StatsImpactType.Co2,
                            title = "CO2 gespart",
                            value = formatCo2(totalCo2Kg),
                            description = "vermiedene Emissionen pro Jahr",
                            quality = "estimated",
                            metaLabel = "Geschätzt",
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
                    capacityClasses = buildCapacityClasses(nationalSummary, parks),
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
            hasLoaded = true
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
        citySummaries: List<RegionSummary>,
        stateSummaries: List<RegionSummary>,
        totalCapacityMw: Double,
    ): List<CityStat> {
        val stateCapacityByName = stateSummaries.associate { it.name to it.installedCapacityKw / 1_000.0 }

        return citySummaries.map { summary ->
            val capacityMw = summary.installedCapacityKw / 1_000.0
            val parentParts = summary.parentName.orEmpty().split(",").map { it.trim() }
            val districtName = parentParts.getOrNull(0).orEmpty()
            val stateName = parentParts.getOrNull(1).orEmpty()
            val stateCapacity = stateCapacityByName[stateName] ?: 0.0
            CityStat(
                cityId = summary.regionId,
                label = summary.name,
                districtName = districtName,
                stateName = stateName,
                windParkCount = summary.windParkCount,
                turbineCount = summary.turbineCount,
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
                municipalBenefitEur = summary.municipalBenefitEur,
            )
        }.sortedByDescending { it.installedCapacityMw }
    }

    private fun buildDistrictStats(
        districtSummaries: List<RegionSummary>,
        stateSummaries: List<RegionSummary>,
        totalCapacityMw: Double,
    ): List<DistrictStat> {
        val stateCapacityByName = stateSummaries.associate { it.name to it.installedCapacityKw / 1_000.0 }

        return districtSummaries.map { summary ->
            val capacityMw = summary.installedCapacityKw / 1_000.0
            val stateName = summary.parentName.orEmpty()
            val stateCapacity = stateCapacityByName[stateName] ?: 0.0
            DistrictStat(
                districtId = summary.regionId,
                label = summary.name,
                contextLabel = summary.contextLabel.orEmpty(),
                stateName = stateName,
                windParkCount = summary.windParkCount,
                turbineCount = summary.turbineCount,
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
                municipalBenefitEur = summary.municipalBenefitEur,
            )
        }.sortedByDescending { it.installedCapacityMw }
    }

    private fun buildStateStats(
        stateSummaries: List<RegionSummary>,
        totalCapacityMw: Double,
    ): List<StateStat> {
        return stateSummaries.map { summary ->
            val capacityMw = summary.installedCapacityKw / 1_000.0
            StateStat(
                stateId = summary.regionId,
                label = summary.name,
                windParkCount = summary.windParkCount,
                turbineCount = summary.turbineCount,
                installedCapacityMw = capacityMw,
                shareOfNationalCapacity = if (totalCapacityMw > 0.0) {
                    (capacityMw / totalCapacityMw).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                },
                municipalBenefitEur = summary.municipalBenefitEur,
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
            districtId = district.districtId,
            label = district.label,
            contextLabel = contextLabel,
            rankText = "Rang $rank von ${districts.size}",
            installedCapacity = formatCapacity(district.installedCapacityMw),
            windParks = formatInteger(district.windParkCount),
            turbines = formatInteger(district.turbineCount),
            nationalShare = formatPercent(district.shareOfNationalCapacity),
            shareProgress = district.shareOfNationalCapacity,
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

    private fun CityStat.comparisonMetrics(): ComparisonMetrics {
        return loadedCitySummariesById[cityId]?.comparisonMetrics()
            ?: comparisonMetricsFromDisplayedStats(installedCapacityMw, turbineCount, municipalBenefitEur)
    }

    private fun DistrictStat.comparisonMetrics(): ComparisonMetrics {
        return loadedDistrictSummariesById[districtId]?.comparisonMetrics()
            ?: comparisonMetricsFromDisplayedStats(installedCapacityMw, turbineCount, municipalBenefitEur)
    }

    private fun StateStat.comparisonMetrics(): ComparisonMetrics {
        return loadedStateSummariesById[stateId]?.comparisonMetrics()
            ?: comparisonMetricsFromDisplayedStats(installedCapacityMw, turbineCount, municipalBenefitEur)
    }

    private fun RegionSummary.comparisonMetrics(): ComparisonMetrics =
        ComparisonMetrics(
            turbines = turbineCount,
            capacityMw = installedCapacityKw / 1_000.0,
            annualProductionKwh = annualProductionKwh,
            co2Kg = co2SavingsKg,
            households = householdEquivalent,
            municipalBenefit = municipalBenefitEur,
        )

    private fun comparisonMetricsFromDisplayedStats(
        capacityMw: Double,
        turbineCount: Int,
        municipalBenefit: Double?,
    ): ComparisonMetrics {
        val annualProduction = capacityMw * 1_000.0 * DEFAULT_FULL_LOAD_HOURS
        return ComparisonMetrics(
            turbines = turbineCount,
            capacityMw = capacityMw,
            annualProductionKwh = annualProduction,
            co2Kg = annualProduction * DEFAULT_EMISSION_FACTOR_KG_PER_KWH,
            households = annualProduction / DEFAULT_HOUSEHOLD_CONSUMPTION_KWH,
            municipalBenefit = municipalBenefit,
        )
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

    private fun buildHouseholdsImpactDetailFromSummary(
        parks: List<WindPark>,
        nationalSummary: NationalStatsSummary?,
        assumptions: List<SnapshotAssumption>,
    ): HouseholdsImpactDetail {
        val totalHouseholds = nationalSummary?.householdEquivalent ?: totalHouseholdsValue
        val topParks = buildParkImpactBars(
            parks = parks,
            assumptions = assumptions,
            selector = { it.households },
            formatter = ::formatCompact,
        )
        val avgPerPark = if ((nationalSummary?.windParkCount ?: parks.size) > 0) {
            totalHouseholds / (nationalSummary?.windParkCount ?: parks.size)
        } else {
            0.0
        }
        val nationalShare = totalHouseholds / GERMAN_HOUSEHOLDS_TOTAL
        return HouseholdsImpactDetail(
            summaryValue = formatCompact(totalHouseholds),
            summarySubtitle = "Haushalte mit durchschnittlichem Jahresverbrauch",
            topParks = topParks,
            nationalSharePercent = "${formatGermanNumber(nationalShare * 100.0, 2)} %",
            avgPerPark = formatCompact(avgPerPark),
            assumptions = listOf(
                StatsImpactFact("Annahme", "3.500 kWh/Jahr je Haushalt"),
                StatsImpactFact("Basis", "vorberechnete Jahresproduktion"),
                StatsImpactFact("Bezug", "ca. ${formatGermanNumber(GERMAN_HOUSEHOLDS_TOTAL / 1_000_000.0, 1)} Mio. Haushalte in Deutschland"),
                StatsImpactFact("Einordnung", "Orientierungswert, kein Liefervertrag"),
            ),
            qualityLabel = formatDataQuality("estimated"),
        )
    }

    private fun buildMunicipalBenefitImpactDetailFromSummary(
        parks: List<WindPark>,
        districts: List<DistrictStat>,
        states: List<StateStat>,
        assumptions: List<SnapshotAssumption>,
    ): MunicipalBenefitImpactDetail {
        val benefitByPark = parks
            .map { it.comparisonMetrics(emptyList(), assumptions).municipalBenefit ?: 0.0 }
            .filter { it > 0.0 }
            .sorted()
        val topDistricts = buildImpactBars(
            districts.mapNotNull { district ->
                district.municipalBenefitEur?.let {
                    ImpactBarInput(district.label, it, ImpactNavigateTarget.Region("district", district.districtId))
                }
            },
            formatter = ::formatCurrency,
        )
        return MunicipalBenefitImpactDetail(
            summaryValue = formatCurrency(loadedNationalSummary?.municipalBenefitEur ?: states.sumOf { it.municipalBenefitEur ?: 0.0 }),
            summarySubtitle = "pro Jahr nach § 6 EEG orientiert",
            topDistricts = topDistricts,
            minPerPark = formatCurrency(benefitByPark.firstOrNull() ?: 0.0),
            medianPerPark = formatCurrency(benefitByPark.medianOrNull() ?: 0.0),
            maxPerPark = formatCurrency(benefitByPark.lastOrNull() ?: 0.0),
            assumptions = listOf(
                StatsImpactFact("Regel", "§ 6 EEG"),
                StatsImpactFact("Richtwert", "bis zu 0,2 ct/kWh"),
                StatsImpactFact("Gilt für", "Windenergie an Land"),
                StatsImpactFact("Einordnung", "Orientierungswert für mögliche Beteiligung"),
            ),
            qualityLabel = formatDataQuality("estimated"),
        )
    }

    private fun buildTurbinesImpactDetailFromSummary(
        parks: List<WindPark>,
        nationalSummary: NationalStatsSummary?,
    ): TurbinesImpactDetail {
        val totalTurbines = nationalSummary?.activeTurbineCount ?: parks.sumOf { it.turbineCount }
        val byDecade = nationalSummary?.let {
            bucketBars(
                listOf(
                    "Vor 2000" to it.turbineCommissioningPre2000.toDouble(),
                    "2000-2009" to it.turbineCommissioning2000To2009.toDouble(),
                    "2010-2019" to it.turbineCommissioning2010To2019.toDouble(),
                    "2020+" to it.turbineCommissioning2020Plus.toDouble(),
                    "Unbekannt" to it.turbineCommissioningUnknown.toDouble(),
                )
            )
        } ?: emptyList()
        val heightBuckets = nationalSummary?.let {
            bucketBars(
                listOf(
                    "< 80 m" to it.turbineHeightLt80m.toDouble(),
                    "80-120 m" to it.turbineHeight80To120m.toDouble(),
                    "120-160 m" to it.turbineHeight120To160m.toDouble(),
                    "> 160 m" to it.turbineHeightGte160m.toDouble(),
                    "Unbekannt" to it.turbineHeightUnknown.toDouble(),
                )
            )
        } ?: emptyList()
        val topParks = buildImpactBars(
            parks.map { ImpactBarInput(it.name, it.turbineCount.toDouble(), ImpactNavigateTarget.Park(it.id)) },
            formatter = { formatGermanNumber(it.roundToInt()) },
        )
        val parkCount = (nationalSummary?.windParkCount ?: parks.size).coerceAtLeast(1)
        return TurbinesImpactDetail(
            summaryValue = formatGermanNumber(totalTurbines),
            summarySubtitle = "aus MaStR/Open-MaStR im lokalen Snapshot",
            byDecade = byDecade,
            heightBuckets = heightBuckets,
            topParks = topParks,
            avgPerPark = "${formatGermanNumber(totalTurbines.toDouble() / parkCount, 1)} Anlagen/Park",
            assumptions = listOf(
                StatsImpactFact("Quelle", "MaStR/Open-MaStR"),
                StatsImpactFact("Einheit", "Windanlage"),
                StatsImpactFact("Darstellung", "Windpark als UX-Einheit"),
                StatsImpactFact("Datenstand", "lokaler Snapshot, keine Live-Daten"),
            ),
            qualityLabel = formatDataQuality("official"),
        )
    }

    private fun buildCo2ImpactDetailFromSummary(
        parks: List<WindPark>,
        nationalSummary: NationalStatsSummary?,
        assumptions: List<SnapshotAssumption>,
    ): Co2ImpactDetail {
        val totalCo2Kg = nationalSummary?.co2SavingsKg ?: totalCo2KgValue
        val topParks = buildParkImpactBars(
            parks = parks,
            assumptions = assumptions,
            selector = { it.co2Kg },
            formatter = { "${formatGermanNumber(it / 1_000.0, 0)} t" },
        )
        return Co2ImpactDetail(
            summaryValue = formatCo2(totalCo2Kg),
            summarySubtitle = "pro Jahr gegenüber Strommix-Emissionen",
            topParks = topParks,
            equivalents = buildCo2Comparisons(totalCo2Kg),
            assumptions = listOf(
                StatsImpactFact("Emissionsfaktor", "380 g/kWh"),
                StatsImpactFact("Basis", "vorberechnete Jahresproduktion"),
                StatsImpactFact("Einordnung", "transparente Schätzung, keine gemessene Einsparung"),
            ),
            qualityLabel = formatDataQuality("estimated"),
        )
    }

    private fun buildParkImpactBars(
        parks: List<WindPark>,
        assumptions: List<SnapshotAssumption>,
        selector: (ComparisonMetrics) -> Double,
        formatter: (Double) -> String,
    ): List<ImpactBarEntry> =
        buildImpactBars(
            parks.map { park ->
                ImpactBarInput(
                    label = park.name,
                    value = selector(park.comparisonMetrics(emptyList(), assumptions)),
                    navigateTarget = ImpactNavigateTarget.Park(park.id),
                )
            },
            formatter = formatter,
        )

    private fun buildImpactBars(
        entries: List<ImpactBarInput>,
        formatter: (Double) -> String,
        limit: Int = 5,
    ): List<ImpactBarEntry> {
        val max = entries.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: 1.0
        return entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { input ->
                ImpactBarEntry(
                    label = input.label,
                    value = formatter(input.value),
                    ratio = (input.value / max).toFloat().coerceIn(0f, 1f),
                    navigateTarget = input.navigateTarget,
                )
            }
    }

    private fun bucketBars(entries: List<Pair<String, Double>>): List<ImpactBarEntry> {
        val visibleEntries = entries.filter { it.second > 0.0 }
        val max = visibleEntries.maxOfOrNull { it.second }?.takeIf { it > 0.0 } ?: 1.0
        return visibleEntries.map { (label, value) ->
            ImpactBarEntry(
                label = label,
                value = formatGermanNumber(value.toInt()),
                ratio = (value / max).toFloat().coerceIn(0f, 1f),
            )
        }
    }

    private fun List<Double>.medianOrNull(): Double? =
        if (isEmpty()) null else {
            val sorted = sorted()
            val mid = size / 2
            if (size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
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

    private fun buildCapacityClasses(
        nationalSummary: NationalStatsSummary?,
        parks: List<WindPark>,
    ): List<CapacityClassStat> {
        val classes = nationalSummary?.let {
            listOf(
                "< 5 MW" to it.capacityClassLt5Mw,
                "5-20 MW" to it.capacityClass5To20Mw,
                "20-50 MW" to it.capacityClass20To50Mw,
                "> 50 MW" to it.capacityClassGte50Mw,
            )
        } ?: listOf(
            "< 5 MW" to parks.count { (it.installedCapacityKw ?: 0L) < 5_000L },
            "5-20 MW" to parks.count { (it.installedCapacityKw ?: 0L) in 5_000L until 20_000L },
            "20-50 MW" to parks.count { (it.installedCapacityKw ?: 0L) in 20_000L until 50_000L },
            "> 50 MW" to parks.count { (it.installedCapacityKw ?: 0L) >= 50_000L },
        )
        val totalCount = (nationalSummary?.windParkCount ?: parks.size).coerceAtLeast(1)
        val maxCount = classes.maxOfOrNull { it.second } ?: 0
        return classes.map { (label, count) ->
            CapacityClassStat(
                label = label,
                description = "Installierte Leistung je Windpark",
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

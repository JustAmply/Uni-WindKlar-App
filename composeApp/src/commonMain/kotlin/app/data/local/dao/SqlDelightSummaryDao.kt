package app.data.local.dao

import app.data.local.source.SourceDatabase
import app.core.model.WindPark
import app.core.model.MapSearchEntry
import app.core.model.RegionSummary
import app.core.model.NationalStatsSummary
import app.data.local.source.Map_search_entry
import app.data.local.source.Region_summary
import app.data.local.source.National_stats_summary

class SqlDelightSummaryDao(
    private val database: SourceDatabase,
) : SummaryDao {

    override suspend fun getMapStartupParks(favorites: Set<String>): Pair<List<WindPark>, Map<String, String>> {
        val statuses = mutableMapOf<String, String>()
        val parks = database.summaryQueries.selectMapStartupParks(
            mapper = {
                id,
                name,
                municipality_id,
                municipality_name,
                district_id,
                district_name,
                state_id,
                state_name,
                latitude,
                longitude,
                grouping_method,
                source_name,
                source_url,
                source_updated_at,
                data_quality,
                park_status,
                valid_turbine_count,
                valid_capacity_kw,
                ->
                statuses[id] = park_status
                WindPark(
                    id = id,
                    name = name,
                    municipalityId = municipality_id,
                    municipalityName = municipality_name,
                    districtId = district_id,
                    districtName = district_name,
                    stateId = state_id,
                    stateName = state_name,
                    latitude = latitude,
                    longitude = longitude,
                    turbineCount = valid_turbine_count.toInt(),
                    installedCapacityKw = valid_capacity_kw,
                    isFavorite = favorites.contains(id),
                    sourceName = source_name,
                    sourceUrl = source_url,
                    sourceUpdatedAt = source_updated_at,
                    dataQuality = data_quality,
                )
            },
        ).executeAsList()
        return Pair(parks, statuses)
    }

    override suspend fun getAllMapSearchEntries(): List<MapSearchEntry> {
        return database.summaryQueries.selectAllMapSearchEntries().executeAsList().map { it.toDomain() }
    }

    override suspend fun getAllParkOperationalSummaries(): Map<String, ParkOperationalSummary> {
        return database.summaryQueries.selectAllParkOperationalSummaries().executeAsList().associate { row ->
            row.wind_park_id to ParkOperationalSummary(
                parkStatus = row.park_status,
                turbineCount = row.valid_turbine_count.toInt(),
                capacityKw = row.valid_capacity_kw
            )
        }
    }

    override suspend fun getRegionSummary(type: String, id: String): RegionSummary? {
        return database.summaryQueries.selectRegionSummary(type.lowercase(), id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getRegionSummaries(type: String): List<RegionSummary> {
        return database.summaryQueries.selectRegionSummaries(type.lowercase()).executeAsList().map { it.toDomain() }
    }

    override suspend fun getNationalStatsSummary(): NationalStatsSummary? {
        return database.summaryQueries.selectNationalStatsSummary().executeAsOneOrNull()?.toDomain()
    }

    private fun Map_search_entry.toDomain(): MapSearchEntry =
        MapSearchEntry(
            id = id,
            resultType = result_type,
            targetId = target_id,
            label = label,
            description = description,
            latitude = latitude,
            longitude = longitude,
            typeRank = type_rank.toInt(),
            haystack = haystack,
            sortName = sort_name,
        )

    private fun Region_summary.toDomain(): RegionSummary =
        RegionSummary(
            regionType = region_type,
            regionId = region_id,
            name = name,
            contextLabel = context_label,
            parentName = parent_name,
            latitude = latitude,
            longitude = longitude,
            windParkCount = wind_park_count.toInt(),
            turbineCount = turbine_count.toInt(),
            installedCapacityKw = installed_capacity_kw,
            annualProductionKwh = annual_production_kwh,
            co2SavingsKg = co2_savings_kg,
            householdEquivalent = household_equivalent,
            municipalBenefitEur = municipal_benefit_eur,
        )

    private fun National_stats_summary.toDomain(): NationalStatsSummary =
        NationalStatsSummary(
            windParkCount = wind_park_count.toInt(),
            activeTurbineCount = active_turbine_count.toInt(),
            installedCapacityKw = installed_capacity_kw,
            annualProductionKwh = annual_production_kwh,
            co2SavingsKg = co2_savings_kg,
            householdEquivalent = household_equivalent,
            municipalBenefitEur = municipal_benefit_eur,
            capacityClassLt5Mw = capacity_class_lt_5mw.toInt(),
            capacityClass5To20Mw = capacity_class_5_20mw.toInt(),
            capacityClass20To50Mw = capacity_class_20_50mw.toInt(),
            capacityClassGte50Mw = capacity_class_gte_50mw.toInt(),
            turbineCommissioningPre2000 = turbine_commissioning_pre_2000.toInt(),
            turbineCommissioning2000To2009 = turbine_commissioning_2000_2009.toInt(),
            turbineCommissioning2010To2019 = turbine_commissioning_2010_2019.toInt(),
            turbineCommissioning2020Plus = turbine_commissioning_2020_plus.toInt(),
            turbineCommissioningUnknown = turbine_commissioning_unknown.toInt(),
            turbineHeightLt80m = turbine_height_lt_80m.toInt(),
            turbineHeight80To120m = turbine_height_80_120m.toInt(),
            turbineHeight120To160m = turbine_height_120_160m.toInt(),
            turbineHeightGte160m = turbine_height_gte_160m.toInt(),
            turbineHeightUnknown = turbine_height_unknown.toInt(),
        )
}

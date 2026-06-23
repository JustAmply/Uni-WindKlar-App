package app.core.model

object ProductionContext {
    const val FULL_LOAD_HOURS_ASSUMPTION_ID = "full_load_hours"

    private const val CALCULATED_FULL_LOAD_HOURS_LABEL = "Berechnete mittlere Volllaststunden"

    fun fullLoadHours(
        annualProductionKwh: Double,
        installedCapacityKw: Double,
    ): Double = if (annualProductionKwh > 0.0 && installedCapacityKw > 0.0) {
        kotlin.math.round(annualProductionKwh / installedCapacityKw)
    } else {
        0.0
    }

    fun assumptionsWithCalculatedFullLoadHours(
        assumptions: List<SnapshotAssumption>,
        fullLoadHours: Double,
        calculationNote: String,
    ): List<SnapshotAssumption> = assumptions.map { assumption ->
        if (assumption.id == FULL_LOAD_HOURS_ASSUMPTION_ID && fullLoadHours > 0.0) {
            assumption.copy(
                value = fullLoadHours,
                label = CALCULATED_FULL_LOAD_HOURS_LABEL,
                calculationNote = calculationNote,
            )
        } else {
            assumption
        }
    }
}

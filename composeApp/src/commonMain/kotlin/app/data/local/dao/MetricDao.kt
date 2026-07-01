package app.data.local.dao

import app.data.local.source.SourceDatabase
import app.data.local.source.Metric as DbMetric
import app.core.model.Metric

interface MetricDao {
    suspend fun getForSubject(subjectType: String, subjectId: String): List<Metric>
    suspend fun getForSubjects(subjectType: String, subjectIds: List<String>): List<Metric>
    suspend fun getAll(): List<Metric>
    suspend fun getNationalAggregates(): List<Metric>
    suspend fun insertOrReplace(metric: Metric)
}

class SqlDelightMetricDao(private val database: SourceDatabase) : MetricDao {
    override suspend fun getForSubject(subjectType: String, subjectId: String): List<Metric> {
        return database.metricQueries.selectMetricsForSubject(subjectType, subjectId).executeAsList().map { it.toDomain() }
    }

    override suspend fun getForSubjects(subjectType: String, subjectIds: List<String>): List<Metric> {
        if (subjectIds.isEmpty()) return emptyList()
        return database.metricQueries.selectMetricsForSubjects(subjectType, subjectIds).executeAsList().map { it.toDomain() }
    }

    override suspend fun getAll(): List<Metric> {
        return database.metricQueries.selectAllMetrics().executeAsList().map { it.toDomain() }
    }

    override suspend fun getNationalAggregates(): List<Metric> {
        return database.metricQueries.selectNationalMetricAggregates().executeAsList().map { row ->
            Metric(
                id = "national_${row.metric_type}",
                subjectType = "national",
                subjectId = "DE",
                metricType = row.metric_type,
                value = if (row.present_value_count > 0L) row.metric_value else null,
                unit = row.unit.orEmpty(),
                period = row.period,
                sourceName = row.source_name ?: "WindKlar MVP-Berechnung",
                sourceUrl = row.source_url.orEmpty(),
                sourceUpdatedAt = row.source_updated_at.orEmpty(),
                dataQuality = row.data_quality,
                calculationNote = row.calculation_note?.let {
                    "Bundesweite Summe aus Windpark-Metriken. $it"
                } ?: "Bundesweite Summe aus Windpark-Metriken.",
            )
        }
    }

    override suspend fun insertOrReplace(metric: Metric) {
        database.metricQueries.upsertMetric(
            id = metric.id,
            subject_type = metric.subjectType,
            subject_id = metric.subjectId,
            metric_type = metric.metricType,
            metric_value = metric.value,
            unit = metric.unit,
            period = metric.period,
            source_name = metric.sourceName,
            source_url = metric.sourceUrl,
            source_updated_at = metric.sourceUpdatedAt,
            data_quality = metric.dataQuality,
            calculation_note = metric.calculationNote
        )
        database.metricQueries.updateMetric(
            id = metric.id,
            subject_type = metric.subjectType,
            subject_id = metric.subjectId,
            metric_type = metric.metricType,
            metric_value = metric.value,
            unit = metric.unit,
            period = metric.period,
            source_name = metric.sourceName,
            source_url = metric.sourceUrl,
            source_updated_at = metric.sourceUpdatedAt,
            data_quality = metric.dataQuality,
            calculation_note = metric.calculationNote
        )
    }

    private fun DbMetric.toDomain() = Metric(
        id = id,
        subjectType = subject_type,
        subjectId = subject_id,
        metricType = metric_type,
        value = metric_value,
        unit = unit,
        period = period,
        sourceName = source_name,
        sourceUrl = source_url,
        sourceUpdatedAt = source_updated_at,
        dataQuality = data_quality,
        calculationNote = calculation_note
    )
}

package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.MetricType
import com.manage.crm.event.domain.TimeWindowUnit
import com.manage.crm.event.domain.repository.projection.CampaignSummaryMetricsProjection
import com.manage.crm.infrastructure.jooq.CrmJooqTables
import com.manage.crm.infrastructure.jooq.JooqR2dbcExecutor
import org.jooq.DSLContext
import org.jooq.impl.DSL.coalesce
import org.jooq.impl.DSL.greatest
import org.jooq.impl.DSL.inline
import org.jooq.impl.DSL.sum
import org.jooq.impl.DSL.`when`
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CampaignDashboardMetricsCustomRepositoryImpl(
    private val dslContext: DSLContext,
    private val jooqExecutor: JooqR2dbcExecutor
) : CampaignDashboardMetricsCustomRepository {

    private val metricsTable = CrmJooqTables.CampaignDashboardMetrics.table
    private val campaignIdField = CrmJooqTables.CampaignDashboardMetrics.campaignId
    private val metricTypeField = CrmJooqTables.CampaignDashboardMetrics.metricType
    private val metricValueField = CrmJooqTables.CampaignDashboardMetrics.metricValue
    private val timeWindowStartField = CrmJooqTables.CampaignDashboardMetrics.timeWindowStart
    private val timeWindowEndField = CrmJooqTables.CampaignDashboardMetrics.timeWindowEnd
    private val timeWindowUnitField = CrmJooqTables.CampaignDashboardMetrics.timeWindowUnit
    private val createdAtField = CrmJooqTables.CampaignDashboardMetrics.createdAt
    private val updatedAtField = CrmJooqTables.CampaignDashboardMetrics.updatedAt

    override suspend fun upsertMetric(
        campaignId: Long,
        metricType: MetricType,
        metricValue: Long,
        timeWindowStart: LocalDateTime,
        timeWindowEnd: LocalDateTime,
        timeWindowUnit: TimeWindowUnit
    ): Int {
        val now = LocalDateTime.now()
        val query = dslContext
            .insertInto(metricsTable)
            .columns(
                campaignIdField,
                metricTypeField,
                metricValueField,
                timeWindowStartField,
                timeWindowEndField,
                timeWindowUnitField,
                createdAtField,
                updatedAtField
            )
            .values(
                campaignId,
                metricType.name,
                metricValue,
                timeWindowStart,
                timeWindowEnd,
                timeWindowUnit.name,
                now,
                now
            )
            .onDuplicateKeyUpdate()
            .set(metricValueField, metricValueField.plus(inline(metricValue)))
            .set(updatedAtField, LocalDateTime.now())

        return jooqExecutor.execute(query)
    }

    override suspend fun upsertMetricAbsolute(
        campaignId: Long,
        metricType: MetricType,
        metricValue: Long,
        timeWindowStart: LocalDateTime,
        timeWindowEnd: LocalDateTime,
        timeWindowUnit: TimeWindowUnit
    ): Int {
        val now = LocalDateTime.now()
        val query = dslContext
            .insertInto(metricsTable)
            .columns(
                campaignIdField,
                metricTypeField,
                metricValueField,
                timeWindowStartField,
                timeWindowEndField,
                timeWindowUnitField,
                createdAtField,
                updatedAtField
            )
            .values(
                campaignId,
                metricType.name,
                metricValue,
                timeWindowStart,
                timeWindowEnd,
                timeWindowUnit.name,
                now,
                now
            )
            .onDuplicateKeyUpdate()
            .set(metricValueField, greatest(metricValueField, inline(metricValue)))
            .set(updatedAtField, LocalDateTime.now())

        return jooqExecutor.execute(query)
    }

    override suspend fun getCampaignSummaryMetrics(
        campaignId: Long,
        last24Hours: LocalDateTime,
        last7Days: LocalDateTime
    ): CampaignSummaryMetricsProjection {
        val totalEventsField = coalesce(sum(metricValueField), 0L).`as`("total_events")
        val eventsLast24HoursField = coalesce(
            sum(
                `when`(timeWindowStartField.gt(last24Hours), metricValueField)
                    .otherwise(0L)
            ),
            0L
        ).`as`("events_last_24_hours")
        val eventsLast7DaysField = coalesce(
            sum(
                `when`(timeWindowStartField.gt(last7Days), metricValueField)
                    .otherwise(0L)
            ),
            0L
        ).`as`("events_last_7_days")

        val query = dslContext
            .select(totalEventsField, eventsLast24HoursField, eventsLast7DaysField)
            .from(metricsTable)
            .where(
                campaignIdField.eq(campaignId)
                    .and(metricTypeField.eq("EVENT_COUNT"))
                    .and(timeWindowUnitField.eq("HOUR"))
            )

        return jooqExecutor.fetchOne(query) { row ->
            CampaignSummaryMetricsProjection(
                totalEvents = (row["total_events"] as Number).toLong(),
                eventsLast24Hours = (row["events_last_24_hours"] as Number).toLong(),
                eventsLast7Days = (row["events_last_7_days"] as Number).toLong()
            )
        } ?: CampaignSummaryMetricsProjection(0, 0, 0)
    }
}

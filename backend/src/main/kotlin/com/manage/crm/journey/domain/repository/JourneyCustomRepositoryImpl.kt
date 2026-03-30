package com.manage.crm.journey.domain.repository

import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository

@Repository
class JourneyCustomRepositoryImpl(
    private val dataBaseClient: DatabaseClient
) : JourneyCustomRepository {
    override suspend fun updateLifecycleStatusIfVersionMatches(
        journeyId: Long,
        lifecycleStatus: String,
        active: Boolean,
        expectedVersion: Int,
        newVersion: Int
    ): Int {
        return dataBaseClient.sql(
            """
            UPDATE journeys
            SET lifecycle_status = :lifecycleStatus,
                active = :active,
                version = :newVersion
            WHERE id = :journeyId
              AND version = :expectedVersion
            """.trimIndent()
        )
            .bind("journeyId", journeyId)
            .bind("lifecycleStatus", lifecycleStatus)
            .bind("active", active)
            .bind("expectedVersion", expectedVersion)
            .bind("newVersion", newVersion)
            .fetch()
            .rowsUpdated()
            .awaitFirst()
            .toInt()
    }
}

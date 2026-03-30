package com.manage.crm.journey.domain.repository

import com.manage.crm.infrastructure.jooq.CrmJooqTables
import com.manage.crm.infrastructure.jooq.JooqR2dbcExecutor
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class JourneyCustomRepositoryImpl(
    private val dslContext: DSLContext,
    private val jooqExecutor: JooqR2dbcExecutor
) : JourneyCustomRepository {
    override suspend fun updateLifecycleStatusIfVersionMatches(
        journeyId: Long,
        lifecycleStatus: String,
        active: Boolean,
        expectedVersion: Int,
        newVersion: Int
    ): Int {
        val query = dslContext
            .update(CrmJooqTables.Journeys.table)
            .set(CrmJooqTables.Journeys.lifecycleStatus, lifecycleStatus)
            .set(CrmJooqTables.Journeys.active, active)
            .set(CrmJooqTables.Journeys.version, newVersion)
            .where(CrmJooqTables.Journeys.id.eq(journeyId))
            .and(CrmJooqTables.Journeys.version.eq(expectedVersion))

        return jooqExecutor.execute(query)
    }
}

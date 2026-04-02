package com.manage.crm.journey.domain.repository

import com.manage.crm.journey.domain.JourneyStep
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface JourneyStepRepository : CoroutineCrudRepository<JourneyStep, Long> {
    fun findAllByJourneyIdOrderByStepOrderAsc(journeyId: Long): Flow<JourneyStep>

    fun findAllByJourneyIdInOrderByJourneyIdAscStepOrderAsc(journeyIds: Collection<Long>): Flow<JourneyStep>
}

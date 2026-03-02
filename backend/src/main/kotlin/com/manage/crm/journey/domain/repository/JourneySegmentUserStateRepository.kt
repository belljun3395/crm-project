package com.manage.crm.journey.domain.repository

import com.manage.crm.journey.domain.JourneySegmentUserState
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface JourneySegmentUserStateRepository : CoroutineCrudRepository<JourneySegmentUserState, Long> {
    suspend fun findAllByJourneyId(journeyId: Long): List<JourneySegmentUserState>
}

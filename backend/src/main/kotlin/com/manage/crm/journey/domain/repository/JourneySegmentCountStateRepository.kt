package com.manage.crm.journey.domain.repository

import com.manage.crm.journey.domain.JourneySegmentCountState
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface JourneySegmentCountStateRepository : CoroutineCrudRepository<JourneySegmentCountState, Long> {
    suspend fun findByJourneyId(journeyId: Long): JourneySegmentCountState?
}

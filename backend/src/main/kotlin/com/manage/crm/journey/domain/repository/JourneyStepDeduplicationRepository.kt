package com.manage.crm.journey.domain.repository

import com.manage.crm.journey.domain.JourneyStepDeduplication
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface JourneyStepDeduplicationRepository : CoroutineCrudRepository<JourneyStepDeduplication, Long>

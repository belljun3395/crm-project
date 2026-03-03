package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import com.manage.crm.support.exception.NotFoundByIdException
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateJourneyLifecycleStatusUseCase(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    suspend fun pause(journeyId: Long): JourneyDto {
        return changeStatus(journeyId, JourneyLifecycleStatus.PAUSED)
    }

    @Transactional
    suspend fun resume(journeyId: Long): JourneyDto {
        return changeStatus(journeyId, JourneyLifecycleStatus.ACTIVE)
    }

    @Transactional
    suspend fun archive(journeyId: Long): JourneyDto {
        return changeStatus(journeyId, JourneyLifecycleStatus.ARCHIVED)
    }

    private suspend fun changeStatus(
        journeyId: Long,
        status: JourneyLifecycleStatus
    ): JourneyDto {
        val journey = journeyRepository.findById(journeyId)
            ?: throw NotFoundByIdException("Journey", journeyId)

        val currentStatus = JourneyLifecycleStatus.from(journey.lifecycleStatus)
        if (currentStatus == JourneyLifecycleStatus.ARCHIVED && status != JourneyLifecycleStatus.ARCHIVED) {
            throw IllegalArgumentException("Archived journey cannot be resumed or paused")
        }
        if (currentStatus == status) {
            val steps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()
            return assembleJourneyDto(journey, steps, objectMapper)
        }

        journey.lifecycleStatus = status.name
        journey.active = status == JourneyLifecycleStatus.ACTIVE
        journey.version = journey.version.coerceAtLeast(1) + 1

        val savedJourney = journeyRepository.save(journey)
        val steps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()
        return assembleJourneyDto(savedJourney, steps, objectMapper)
    }
}

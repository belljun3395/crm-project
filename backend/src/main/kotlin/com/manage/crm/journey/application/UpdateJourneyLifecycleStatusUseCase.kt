package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.application.dto.JourneyDto
import com.manage.crm.journey.application.dto.JourneyLifecycleAction
import com.manage.crm.journey.application.dto.JourneyLifecycleStatus
import com.manage.crm.journey.application.dto.UpdateJourneyLifecycleStatusUseCaseIn
import com.manage.crm.journey.application.dto.UpdateJourneyLifecycleStatusUseCaseOut
import com.manage.crm.journey.application.dto.toJourneyDto
import com.manage.crm.journey.application.dto.toJourneyStepDto
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import com.manage.crm.journey.exception.InvalidJourneyException
import com.manage.crm.support.exception.NotFoundByIdException
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * UC-JOURNEY-006
 * Handles lifecycle transitions for journeys and increments the version on change.
 *
 * Input: journey id and lifecycle action (pause/resume/archive).
 * Success: applies allowed state transition and returns the updated journey DTO.
 */
@Component
class UpdateJourneyLifecycleStatusUseCase(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    suspend fun execute(useCaseIn: UpdateJourneyLifecycleStatusUseCaseIn): UpdateJourneyLifecycleStatusUseCaseOut {
        val targetStatus =
            when (useCaseIn.action) {
                JourneyLifecycleAction.PAUSE -> JourneyLifecycleStatus.PAUSED
                JourneyLifecycleAction.RESUME -> JourneyLifecycleStatus.ACTIVE
                JourneyLifecycleAction.ARCHIVE -> JourneyLifecycleStatus.ARCHIVED
            }

        return UpdateJourneyLifecycleStatusUseCaseOut(changeStatus(useCaseIn.journeyId, targetStatus))
    }

    private suspend fun changeStatus(
        journeyId: Long,
        status: JourneyLifecycleStatus,
    ): JourneyDto {
        val journey =
            journeyRepository.findById(journeyId)
                ?: throw NotFoundByIdException("Journey", journeyId)

        val currentStatus = JourneyLifecycleStatus.from(journey.lifecycleStatus)
        if (currentStatus == JourneyLifecycleStatus.ARCHIVED && status != JourneyLifecycleStatus.ARCHIVED) {
            throw InvalidJourneyException("Archived journey cannot be resumed or paused")
        }
        if (currentStatus == status) {
            val steps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()
            return journey.toJourneyDto(steps.map { it.toJourneyStepDto(objectMapper) }, objectMapper)
        }

        val expectedVersion = journey.version
        val newVersion = expectedVersion.coerceAtLeast(1) + 1
        val updatedRows =
            journeyRepository.updateLifecycleStatusIfVersionMatches(
                journeyId = journeyId,
                lifecycleStatus = status.name,
                active = status == JourneyLifecycleStatus.ACTIVE,
                expectedVersion = expectedVersion,
                newVersion = newVersion,
            )
        if (updatedRows == 0) {
            throw IllegalStateException("Journey lifecycle update conflict detected. Please retry.")
        }

        val savedJourney =
            journeyRepository.findById(journeyId)
                ?: throw NotFoundByIdException("Journey", journeyId)
        val steps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()
        return savedJourney.toJourneyDto(steps.map { it.toJourneyStepDto(objectMapper) }, objectMapper)
    }
}

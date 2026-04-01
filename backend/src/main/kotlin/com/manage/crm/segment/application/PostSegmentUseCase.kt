package com.manage.crm.segment.application

import com.manage.crm.journey.queue.JourneyTriggerQueuePublisher
import com.manage.crm.segment.application.dto.PostSegmentConditionIn
import com.manage.crm.segment.application.dto.PostSegmentUseCaseIn
import com.manage.crm.segment.application.dto.PostSegmentUseCaseOut
import com.manage.crm.segment.application.dto.toSegmentConditionDto
import com.manage.crm.segment.application.dto.toSegmentDto
import com.manage.crm.segment.domain.Segment
import com.manage.crm.segment.domain.SegmentCondition
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.segment.exception.InvalidSegmentConditionException
import com.manage.crm.segment.util.SegmentConditionValidator
import com.manage.crm.support.exception.AlreadyExistsException
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import com.manage.crm.support.transactional.TransactionSynchronizationTemplate
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * UC-SEGMENT-001
 * Creates or updates a segment and atomically replaces condition definitions.
 *
 * Input: segment create/update payload including condition list.
 * Success: persists segment and returns saved segment snapshot.
 * Failure: throws AlreadyExistsException for duplicate name,
 *          InvalidSegmentConditionException for invalid condition payload,
 *          NotFoundByIdException when update target does not exist.
 * Side effects: publishes journey trigger after commit.
 */
@Component
class PostSegmentUseCase(
    private val segmentRepository: SegmentRepository,
    private val segmentConditionRepository: SegmentConditionRepository,
    private val journeyTriggerQueuePublisher: JourneyTriggerQueuePublisher,
    private val transactionSynchronizationTemplate: TransactionSynchronizationTemplate,
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    suspend fun execute(useCaseIn: PostSegmentUseCaseIn): PostSegmentUseCaseOut {
        validateConditions(useCaseIn.conditions)

        val duplicated = segmentRepository.findByName(useCaseIn.name)
        if (useCaseIn.id == null && duplicated != null) {
            throw AlreadyExistsException("Segment", "name", useCaseIn.name)
        }
        if (useCaseIn.id != null && duplicated != null && duplicated.id != useCaseIn.id) {
            throw AlreadyExistsException("Segment", "name", useCaseIn.name)
        }

        val saved =
            try {
                if (useCaseIn.id != null) {
                    val existing = segmentRepository.findById(useCaseIn.id) ?: throw NotFoundByIdException("Segment", useCaseIn.id)
                    existing.name = useCaseIn.name
                    existing.description = useCaseIn.description
                    existing.active = useCaseIn.active
                    segmentRepository.save(existing)
                } else {
                    segmentRepository.save(
                        Segment.new(
                            name = useCaseIn.name,
                            description = useCaseIn.description,
                            active = useCaseIn.active,
                        ),
                    )
                }
            } catch (error: DataIntegrityViolationException) {
                if (isSegmentNameDuplicate(error)) {
                    throw AlreadyExistsException("Segment", "name", useCaseIn.name)
                }
                throw error
            }

        val segmentId = saved.id ?: throw IllegalStateException("Saved segment id is null")
        segmentConditionRepository.deleteBySegmentId(segmentId)
        useCaseIn.conditions.forEachIndexed { index, condition ->
            segmentConditionRepository.save(
                SegmentCondition.new(
                    segmentId = segmentId,
                    fieldName = condition.field,
                    operator = condition.operator.uppercase(),
                    valueType = condition.valueType.uppercase(),
                    conditionValue = condition.value.toString(),
                    position = index + 1,
                ),
            )
        }

        runCatching {
            transactionSynchronizationTemplate.afterCommit(
                blockDescription = "enqueue journey segment trigger after segment commit",
            ) {
                journeyTriggerQueuePublisher.publishSegmentContextTrigger()
            }
        }.onFailure { error ->
            log.error(error) {
                "Failed to register afterCommit segment context trigger for segmentId=$segmentId"
            }
        }

        val conditionDtos =
            useCaseIn.conditions.mapIndexed { index, condition ->
                condition.toSegmentConditionDto(position = index + 1)
            }
        return out {
            PostSegmentUseCaseOut(
                segment = saved.toSegmentDto(conditionDtos),
            )
        }
    }

    private fun validateConditions(conditions: List<PostSegmentConditionIn>) {
        if (conditions.isEmpty()) {
            throw InvalidSegmentConditionException("conditions must not be empty")
        }
        conditions.forEach { condition ->
            SegmentConditionValidator.validate(
                field = condition.field,
                operator = condition.operator,
                valueType = condition.valueType,
                value = condition.value,
            )
        }
    }

    private fun isSegmentNameDuplicate(exception: DataIntegrityViolationException): Boolean {
        var cause: Throwable? = exception
        while (cause != null) {
            val message = cause.message?.lowercase()
            if (message != null && "uq_segments_name" in message) {
                return true
            }
            cause = cause.cause
        }
        return false
    }
}

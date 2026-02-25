package com.manage.crm.segment.application

import com.manage.crm.segment.application.dto.PostSegmentConditionIn
import com.manage.crm.segment.application.dto.PostSegmentUseCaseIn
import com.manage.crm.segment.application.dto.PostSegmentUseCaseOut
import com.manage.crm.segment.application.dto.SegmentConditionDto
import com.manage.crm.segment.application.dto.SegmentDto
import com.manage.crm.segment.domain.Segment
import com.manage.crm.segment.domain.SegmentCondition
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.segment.exception.InvalidSegmentConditionException
import com.manage.crm.support.exception.AlreadyExistsException
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

/**
 * Creates or updates a segment and replaces its condition list atomically.
 */
@Service
class PostSegmentUseCase(
    private val segmentRepository: SegmentRepository,
    private val segmentConditionRepository: SegmentConditionRepository
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

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

        val saved = try {
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
                        active = useCaseIn.active
                    )
                )
            }
        } catch (_: DataIntegrityViolationException) {
            throw AlreadyExistsException("Segment", "name", useCaseIn.name)
        }

        val segmentId = saved.id ?: throw NotFoundByIdException("Segment", -1)
        replaceConditions(segmentId, useCaseIn.conditions)

        val conditionDtos = useCaseIn.conditions.mapIndexed { index, condition ->
            SegmentConditionDto(
                field = condition.field,
                operator = condition.operator.uppercase(),
                valueType = condition.valueType.uppercase(),
                value = condition.value,
                position = index + 1
            )
        }
        return out {
            PostSegmentUseCaseOut(
                segment = SegmentDto(
                    id = segmentId,
                    name = saved.name,
                    description = saved.description,
                    active = saved.active,
                    conditions = conditionDtos,
                    createdAt = saved.createdAt?.format(formatter)
                )
            )
        }
    }

    private suspend fun replaceConditions(
        segmentId: Long,
        conditions: List<PostSegmentConditionIn>
    ) {
        segmentConditionRepository.deleteBySegmentId(segmentId)

        conditions.forEachIndexed { index, condition ->
            segmentConditionRepository.save(
                SegmentCondition.new(
                    segmentId = segmentId,
                    fieldName = condition.field,
                    operator = condition.operator.uppercase(),
                    valueType = condition.valueType.uppercase(),
                    conditionValue = condition.value.toString(),
                    position = index + 1
                )
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
                value = condition.value
            )
        }
    }
}

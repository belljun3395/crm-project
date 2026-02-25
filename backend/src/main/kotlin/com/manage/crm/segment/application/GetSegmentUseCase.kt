package com.manage.crm.segment.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.manage.crm.segment.application.dto.GetSegmentUseCaseIn
import com.manage.crm.segment.application.dto.GetSegmentUseCaseOut
import com.manage.crm.segment.application.dto.SegmentConditionDto
import com.manage.crm.segment.application.dto.SegmentDto
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

/**
 * Retrieves a single segment with its ordered conditions.
 */
@Service
class GetSegmentUseCase(
    private val segmentRepository: SegmentRepository,
    private val segmentConditionRepository: SegmentConditionRepository,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    suspend fun execute(useCaseIn: GetSegmentUseCaseIn): GetSegmentUseCaseOut {
        val segment = segmentRepository.findById(useCaseIn.id) ?: throw NotFoundByIdException("Segment", useCaseIn.id)
        val conditions = segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segment.id!!)
            .toList()
            .map { condition ->
                SegmentConditionDto(
                    field = condition.fieldName,
                    operator = condition.operator,
                    valueType = condition.valueType,
                    value = runCatching { objectMapper.readTree(condition.conditionValue) }.getOrElse { NullNode.instance },
                    position = condition.position
                )
            }

        return out {
            GetSegmentUseCaseOut(
                segment = SegmentDto(
                    id = segment.id!!,
                    name = segment.name,
                    description = segment.description,
                    active = segment.active,
                    conditions = conditions,
                    createdAt = segment.createdAt?.format(formatter)
                )
            )
        }
    }
}

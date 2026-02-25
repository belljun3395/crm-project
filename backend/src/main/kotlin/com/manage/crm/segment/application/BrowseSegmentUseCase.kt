package com.manage.crm.segment.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.manage.crm.segment.application.dto.BrowseSegmentUseCaseIn
import com.manage.crm.segment.application.dto.BrowseSegmentUseCaseOut
import com.manage.crm.segment.application.dto.SegmentConditionDto
import com.manage.crm.segment.application.dto.SegmentDto
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.out
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
/**
 * Lists segments with their ordered condition set.
 */
class BrowseSegmentUseCase(
    private val segmentRepository: SegmentRepository,
    private val segmentConditionRepository: SegmentConditionRepository,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 200
    }

    suspend fun execute(useCaseIn: BrowseSegmentUseCaseIn): BrowseSegmentUseCaseOut {
        val normalizedLimit = useCaseIn.limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        val segments = segmentRepository.findAllByOrderByCreatedAtDesc()
            .take(normalizedLimit)
            .toList()
        val segmentIds = segments.mapNotNull { it.id }
        val conditionsBySegmentId = if (segmentIds.isEmpty()) {
            emptyMap()
        } else {
            segmentConditionRepository.findBySegmentIdInOrderBySegmentIdAscPositionAsc(segmentIds)
                .toList()
                .groupBy { it.segmentId }
        }

        val segmentDtos = segments
            .map { segment ->
                val segmentId = segment.id!!
                val conditions = conditionsBySegmentId[segmentId].orEmpty()
                    .map { condition ->
                        SegmentConditionDto(
                            field = condition.fieldName,
                            operator = condition.operator,
                            valueType = condition.valueType,
                            value = runCatching { objectMapper.readTree(condition.conditionValue) }.getOrElse { NullNode.instance },
                            position = condition.position
                        )
                    }
                SegmentDto(
                    id = segmentId,
                    name = segment.name,
                    description = segment.description,
                    active = segment.active,
                    conditions = conditions,
                    createdAt = segment.createdAt?.format(formatter)
                )
            }

        return out {
            BrowseSegmentUseCaseOut(segments = segmentDtos)
        }
    }
}

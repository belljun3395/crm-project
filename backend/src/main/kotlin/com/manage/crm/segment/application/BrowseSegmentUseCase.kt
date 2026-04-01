package com.manage.crm.segment.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.segment.application.dto.BrowseSegmentUseCaseIn
import com.manage.crm.segment.application.dto.BrowseSegmentUseCaseOut
import com.manage.crm.segment.application.dto.toSegmentConditionDto
import com.manage.crm.segment.application.dto.toSegmentDto
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.out
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

/**
 * UC-SEGMENT-002
 * Lists segments with ordered conditions.
 *
 * Input: list limit.
 * Success: returns segments sorted by createdAt desc with per-segment ordered conditions.
 */
@Component
class BrowseSegmentUseCase(
    private val segmentRepository: SegmentRepository,
    private val segmentConditionRepository: SegmentConditionRepository,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 200
    }

    suspend fun execute(useCaseIn: BrowseSegmentUseCaseIn): BrowseSegmentUseCaseOut {
        val normalizedLimit = useCaseIn.limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        val segments =
            segmentRepository
                .findAllByOrderByCreatedAtDesc()
                .take(normalizedLimit)
                .toList()
        val segmentIds = segments.mapNotNull { it.id }
        val conditionsBySegmentId =
            if (segmentIds.isEmpty()) {
                emptyMap()
            } else {
                segmentConditionRepository
                    .findBySegmentIdInOrderBySegmentIdAscPositionAsc(segmentIds)
                    .toList()
                    .groupBy { it.segmentId }
            }

        val segmentDtos =
            segments
                .map { segment ->
                    val segmentId = segment.id ?: throw IllegalStateException("Segment id is null")
                    val conditions =
                        conditionsBySegmentId[segmentId]
                            .orEmpty()
                            .map { condition -> condition.toSegmentConditionDto(objectMapper) }
                    segment.toSegmentDto(conditions)
                }

        return out {
            BrowseSegmentUseCaseOut(segments = segmentDtos)
        }
    }
}

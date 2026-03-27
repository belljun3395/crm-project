package com.manage.crm.segment.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.segment.application.dto.GetSegmentUseCaseIn
import com.manage.crm.segment.application.dto.GetSegmentUseCaseOut
import com.manage.crm.segment.application.dto.toSegmentConditionDto
import com.manage.crm.segment.application.dto.toSegmentDto
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

/**
 * UC-SEGMENT-003
 * Retrieves a segment detail with ordered conditions.
 *
 * Input: segment id.
 * Success: returns the segment with condition list sorted by position asc.
 * Failure: throws NotFoundByIdException when segment does not exist.
 */
@Component
class GetSegmentUseCase(
    private val segmentRepository: SegmentRepository,
    private val segmentConditionRepository: SegmentConditionRepository,
    private val objectMapper: ObjectMapper
) {
    suspend fun execute(useCaseIn: GetSegmentUseCaseIn): GetSegmentUseCaseOut {
        val segment = segmentRepository.findById(useCaseIn.id) ?: throw NotFoundByIdException("Segment", useCaseIn.id)
        val segmentId = segment.id ?: throw IllegalStateException("Segment id is null")
        val conditions = segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId)
            .toList()
            .map { condition -> condition.toSegmentConditionDto(objectMapper) }

        return out {
            GetSegmentUseCaseOut(
                segment = segment.toSegmentDto(conditions)
            )
        }
    }
}

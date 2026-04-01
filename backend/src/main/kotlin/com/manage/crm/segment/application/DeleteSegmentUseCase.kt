package com.manage.crm.segment.application

import com.manage.crm.segment.application.dto.DeleteSegmentUseCaseIn
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.exception.NotFoundByIdException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * UC-SEGMENT-004
 * Deletes a segment.
 *
 * Input: segment id.
 * Success: removes segment row (segment_conditions are cascade-deleted by FK).
 * Failure: throws NotFoundByIdException when segment does not exist.
 */
@Component
class DeleteSegmentUseCase(
    private val segmentRepository: SegmentRepository,
) {
    @Transactional
    suspend fun execute(input: DeleteSegmentUseCaseIn) {
        val segment = segmentRepository.findById(input.id) ?: throw NotFoundByIdException("Segment", input.id)
        segmentRepository.delete(segment)
    }
}

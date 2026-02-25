package com.manage.crm.segment.application

import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.exception.NotFoundByIdException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Deletes a segment and its associated conditions.
 */
@Service
class DeleteSegmentUseCase(
    private val segmentRepository: SegmentRepository
) {
    @Transactional
    suspend fun execute(segmentId: Long) {
        val segment = segmentRepository.findById(segmentId) ?: throw NotFoundByIdException("Segment", segmentId)
        segmentRepository.delete(segment)
    }
}

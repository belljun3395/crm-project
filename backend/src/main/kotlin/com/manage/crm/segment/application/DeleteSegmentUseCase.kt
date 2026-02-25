package com.manage.crm.segment.application

import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.exception.NotFoundByIdException
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteSegmentUseCase(
    private val segmentRepository: SegmentRepository,
    private val segmentConditionRepository: SegmentConditionRepository
) {
    @Transactional
    suspend fun execute(segmentId: Long) {
        val segment = segmentRepository.findById(segmentId) ?: throw NotFoundByIdException("Segment", segmentId)
        segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId)
            .toList()
            .forEach { segmentConditionRepository.delete(it) }
        segmentRepository.delete(segment)
    }
}

package com.manage.crm.segment.adapter.query

import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.segment.service.SegmentTargetingService
import org.springframework.stereotype.Component

@Component
class SegmentReadAdapter(
    private val segmentRepository: SegmentRepository,
    private val segmentTargetingService: SegmentTargetingService
) : SegmentReadPort {
    override suspend fun existsById(segmentId: Long): Boolean {
        return segmentRepository.findById(segmentId) != null
    }

    override suspend fun findNameById(segmentId: Long): String? {
        return segmentRepository.findById(segmentId)?.name
    }

    override suspend fun findTargetUserIds(segmentId: Long, campaignId: Long?): List<Long> {
        return segmentTargetingService.resolveUserIds(segmentId, campaignId)
    }
}

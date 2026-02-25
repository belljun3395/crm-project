package com.manage.crm.event.application

interface SegmentTargetingService {
    suspend fun resolveUserIds(segmentId: Long): List<Long>
}

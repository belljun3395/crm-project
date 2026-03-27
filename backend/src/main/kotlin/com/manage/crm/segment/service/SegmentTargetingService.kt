package com.manage.crm.segment.service

interface SegmentTargetingService {
    suspend fun resolveUserIds(segmentId: Long, campaignId: Long?): List<Long>
}

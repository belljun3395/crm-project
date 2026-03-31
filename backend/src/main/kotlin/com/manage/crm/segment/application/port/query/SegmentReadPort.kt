package com.manage.crm.segment.application.port.query

interface SegmentReadPort {
    suspend fun existsById(segmentId: Long): Boolean

    suspend fun findNameById(segmentId: Long): String?

    suspend fun findTargetUserIds(segmentId: Long, campaignId: Long?): List<Long>
}

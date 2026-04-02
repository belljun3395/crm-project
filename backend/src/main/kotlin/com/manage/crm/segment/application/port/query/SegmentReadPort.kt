package com.manage.crm.segment.application.port.query

import java.time.LocalDateTime

data class SegmentTargetUserReadModel(
    val id: Long,
    val userAttributesJson: String,
    val createdAt: LocalDateTime?,
)

data class SegmentTargetEventReadModel(
    val userId: Long,
    val name: String,
    val occurredAt: LocalDateTime?,
)

interface SegmentReadPort {
    suspend fun existsById(segmentId: Long): Boolean

    suspend fun findNameById(segmentId: Long): String?

    suspend fun findTargetUserIds(
        segmentId: Long,
        users: List<SegmentTargetUserReadModel>,
        eventsByUserId: Map<Long, List<SegmentTargetEventReadModel>> = emptyMap(),
    ): List<Long>

    suspend fun findTargetUserIds(segmentId: Long): List<Long>
}

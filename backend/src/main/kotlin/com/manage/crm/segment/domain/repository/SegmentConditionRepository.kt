package com.manage.crm.segment.domain.repository

import com.manage.crm.segment.domain.SegmentCondition
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface SegmentConditionRepository : CoroutineCrudRepository<SegmentCondition, Long> {
    /**
     * Streams conditions for a segment in configured evaluation order.
     */
    fun findBySegmentIdOrderByPositionAsc(segmentId: Long): Flow<SegmentCondition>

    /**
     * Streams conditions for multiple segments, grouped by segment id and position order.
     */
    fun findBySegmentIdInOrderBySegmentIdAscPositionAsc(segmentIds: Collection<Long>): Flow<SegmentCondition>

    /**
     * Deletes all conditions belonging to a segment.
     */
    suspend fun deleteBySegmentId(segmentId: Long): Long
}

package com.manage.crm.segment.domain.repository

import com.manage.crm.segment.domain.SegmentCondition
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface SegmentConditionRepository : CoroutineCrudRepository<SegmentCondition, Long> {
    fun findBySegmentIdOrderByPositionAsc(segmentId: Long): Flow<SegmentCondition>
}

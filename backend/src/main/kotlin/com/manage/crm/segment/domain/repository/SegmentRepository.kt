package com.manage.crm.segment.domain.repository

import com.manage.crm.segment.domain.Segment
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface SegmentRepository : CoroutineCrudRepository<Segment, Long> {
    suspend fun findByName(name: String): Segment?
    fun findAllByOrderByCreatedAtDesc(): Flow<Segment>
}

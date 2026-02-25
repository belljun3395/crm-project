package com.manage.crm.segment.domain.repository

import com.manage.crm.segment.domain.Segment
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface SegmentRepository : CoroutineCrudRepository<Segment, Long> {
    /**
     * Finds a segment by unique name.
     */
    suspend fun findByName(name: String): Segment?

    /**
     * Streams all segments from newest to oldest.
     */
    fun findAllByOrderByCreatedAtDesc(): Flow<Segment>
}

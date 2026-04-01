package com.manage.crm.audit.domain.repository

import com.manage.crm.audit.domain.AuditLog
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AuditLogRepository : CoroutineCrudRepository<AuditLog, Long> {
    /**
     * Streams audit logs from newest to oldest.
     */
    fun findAllByOrderByCreatedAtDesc(): Flow<AuditLog>

    /**
     * Streams audit logs filtered by action.
     */
    fun findByActionOrderByCreatedAtDesc(action: String): Flow<AuditLog>

    /**
     * Streams audit logs filtered by resource type.
     */
    fun findByResourceTypeOrderByCreatedAtDesc(resourceType: String): Flow<AuditLog>

    /**
     * Streams audit logs filtered by actor id.
     */
    fun findByActorIdOrderByCreatedAtDesc(actorId: String): Flow<AuditLog>

    /**
     * Streams audit logs filtered by action and resource type.
     */
    fun findByActionAndResourceTypeOrderByCreatedAtDesc(
        action: String,
        resourceType: String,
    ): Flow<AuditLog>

    /**
     * Streams audit logs filtered by action and actor id.
     */
    fun findByActionAndActorIdOrderByCreatedAtDesc(
        action: String,
        actorId: String,
    ): Flow<AuditLog>

    /**
     * Streams audit logs filtered by resource type and actor id.
     */
    fun findByResourceTypeAndActorIdOrderByCreatedAtDesc(
        resourceType: String,
        actorId: String,
    ): Flow<AuditLog>

    /**
     * Streams audit logs filtered by action, resource type, and actor id.
     */
    fun findByActionAndResourceTypeAndActorIdOrderByCreatedAtDesc(
        action: String,
        resourceType: String,
        actorId: String,
    ): Flow<AuditLog>
}

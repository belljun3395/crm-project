package com.manage.crm.audit.domain.repository

import com.manage.crm.audit.domain.AuditLog
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AuditLogRepository : CoroutineCrudRepository<AuditLog, Long> {
    fun findAllByOrderByCreatedAtDesc(): Flow<AuditLog>
    fun findByActionOrderByCreatedAtDesc(action: String): Flow<AuditLog>
    fun findByResourceTypeOrderByCreatedAtDesc(resourceType: String): Flow<AuditLog>
    fun findByActorIdOrderByCreatedAtDesc(actorId: String): Flow<AuditLog>
}

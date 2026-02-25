package com.manage.crm.audit.application

import com.manage.crm.audit.application.dto.AuditLogDto
import com.manage.crm.audit.application.dto.BrowseAuditLogsUseCaseIn
import com.manage.crm.audit.application.dto.BrowseAuditLogsUseCaseOut
import com.manage.crm.audit.domain.AuditLog
import com.manage.crm.audit.domain.repository.AuditLogRepository
import com.manage.crm.support.out
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
/**
 * Lists audit log entries with optional action/resource/actor filters.
 */
class BrowseAuditLogsUseCase(
    private val auditLogRepository: AuditLogRepository
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 200
    }

    suspend fun execute(useCaseIn: BrowseAuditLogsUseCaseIn): BrowseAuditLogsUseCaseOut {
        val normalizedLimit = useCaseIn.limit.coerceIn(MIN_LIMIT, MAX_LIMIT)

        val logs = selectBaseFlow(useCaseIn)
            .take(normalizedLimit)
            .toList()
            .map { log ->
                AuditLogDto(
                    id = log.id!!,
                    actorId = log.actorId,
                    action = log.action,
                    resourceType = log.resourceType,
                    resourceId = log.resourceId,
                    requestMethod = log.requestMethod,
                    requestPath = log.requestPath,
                    statusCode = log.statusCode,
                    detail = log.detail,
                    createdAt = log.createdAt?.format(formatter)
                )
            }

        return out {
            BrowseAuditLogsUseCaseOut(logs)
        }
    }

    private fun selectBaseFlow(useCaseIn: BrowseAuditLogsUseCaseIn): Flow<AuditLog> {
        val action = useCaseIn.action
        val resourceType = useCaseIn.resourceType
        val actorId = useCaseIn.actorId

        return when {
            action != null && resourceType != null && actorId != null ->
                auditLogRepository.findByActionAndResourceTypeAndActorIdOrderByCreatedAtDesc(
                    action = action,
                    resourceType = resourceType,
                    actorId = actorId
                )

            action != null && resourceType != null ->
                auditLogRepository.findByActionAndResourceTypeOrderByCreatedAtDesc(
                    action = action,
                    resourceType = resourceType
                )

            action != null && actorId != null ->
                auditLogRepository.findByActionAndActorIdOrderByCreatedAtDesc(
                    action = action,
                    actorId = actorId
                )

            resourceType != null && actorId != null ->
                auditLogRepository.findByResourceTypeAndActorIdOrderByCreatedAtDesc(
                    resourceType = resourceType,
                    actorId = actorId
                )

            action != null -> auditLogRepository.findByActionOrderByCreatedAtDesc(action)

            resourceType != null -> auditLogRepository.findByResourceTypeOrderByCreatedAtDesc(resourceType)

            actorId != null -> auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId)

            else -> auditLogRepository.findAllByOrderByCreatedAtDesc()
        }
    }
}

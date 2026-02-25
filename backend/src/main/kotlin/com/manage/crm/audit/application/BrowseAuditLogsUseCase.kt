package com.manage.crm.audit.application

import com.manage.crm.audit.application.dto.AuditLogDto
import com.manage.crm.audit.application.dto.BrowseAuditLogsUseCaseIn
import com.manage.crm.audit.application.dto.BrowseAuditLogsUseCaseOut
import com.manage.crm.audit.domain.AuditLog
import com.manage.crm.audit.domain.repository.AuditLogRepository
import com.manage.crm.support.out
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
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

        val baseFlow = selectBaseFlow(useCaseIn)
        val logs = baseFlow
            .filter { log ->
                (useCaseIn.action == null || log.action == useCaseIn.action) &&
                    (useCaseIn.resourceType == null || log.resourceType == useCaseIn.resourceType) &&
                    (useCaseIn.actorId == null || log.actorId == useCaseIn.actorId)
            }
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
        return when {
            useCaseIn.action != null && useCaseIn.resourceType == null && useCaseIn.actorId == null ->
                auditLogRepository.findByActionOrderByCreatedAtDesc(useCaseIn.action)

            useCaseIn.resourceType != null && useCaseIn.action == null && useCaseIn.actorId == null ->
                auditLogRepository.findByResourceTypeOrderByCreatedAtDesc(useCaseIn.resourceType)

            useCaseIn.actorId != null && useCaseIn.action == null && useCaseIn.resourceType == null ->
                auditLogRepository.findByActorIdOrderByCreatedAtDesc(useCaseIn.actorId)

            else -> auditLogRepository.findAllByOrderByCreatedAtDesc()
        }
    }
}

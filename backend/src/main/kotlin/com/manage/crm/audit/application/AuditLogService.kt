package com.manage.crm.audit.application

import com.manage.crm.audit.application.dto.RecordAuditLogCommand
import com.manage.crm.audit.domain.AuditLog
import com.manage.crm.audit.domain.repository.AuditLogRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepository
) {
    private val log = KotlinLogging.logger {}

    suspend fun record(command: RecordAuditLogCommand) {
        runCatching {
            auditLogRepository.save(
                AuditLog.new(
                    actorId = command.actorId,
                    action = command.action,
                    resourceType = command.resourceType,
                    resourceId = command.resourceId,
                    requestMethod = command.requestMethod,
                    requestPath = command.requestPath,
                    statusCode = command.statusCode,
                    detail = command.detail
                )
            )
        }.onFailure { error ->
            log.error(error) {
                "Failed to save audit log: action=${command.action}, resourceType=${command.resourceType}, resourceId=${command.resourceId}"
            }
        }
    }
}

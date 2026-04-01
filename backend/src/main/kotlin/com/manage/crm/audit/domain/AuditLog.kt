package com.manage.crm.audit.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

/**
 * Persistence model representing an immutable audit event record.
 */
@Table("audit_logs")
class AuditLog(
    @Id
    var id: Long? = null,
    @Column("actor_id")
    var actorId: String? = null,
    @Column("action")
    var action: String,
    @Column("resource_type")
    var resourceType: String,
    @Column("resource_id")
    var resourceId: String? = null,
    @Column("request_method")
    var requestMethod: String? = null,
    @Column("request_path")
    var requestPath: String? = null,
    @Column("status_code")
    var statusCode: Int? = null,
    @Column("detail")
    var detail: String? = null,
    @CreatedDate
    @Column("created_at")
    var createdAt: LocalDateTime? = null,
) {
    companion object {
        fun new(
            actorId: String?,
            action: String,
            resourceType: String,
            resourceId: String?,
            requestMethod: String?,
            requestPath: String?,
            statusCode: Int?,
            detail: String?,
        ): AuditLog =
            AuditLog(
                actorId = actorId,
                action = action,
                resourceType = resourceType,
                resourceId = resourceId,
                requestMethod = requestMethod,
                requestPath = requestPath,
                statusCode = statusCode,
                detail = detail,
            )
    }
}

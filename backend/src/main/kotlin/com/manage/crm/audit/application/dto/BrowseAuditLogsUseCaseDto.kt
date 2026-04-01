package com.manage.crm.audit.application.dto

data class BrowseAuditLogsUseCaseIn(
    val limit: Int = 50,
    val action: String? = null,
    val resourceType: String? = null,
    val actorId: String? = null,
)

data class BrowseAuditLogsUseCaseOut(
    val logs: List<AuditLogDto>,
)

data class AuditLogDto(
    val id: Long,
    val actorId: String?,
    val action: String,
    val resourceType: String,
    val resourceId: String?,
    val requestMethod: String?,
    val requestPath: String?,
    val statusCode: Int?,
    val detail: String?,
    val createdAt: String?,
)

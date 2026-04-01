package com.manage.crm.audit.application.dto

data class RecordAuditLogCommand(
    val actorId: String?,
    val action: String,
    val resourceType: String,
    val resourceId: String?,
    val requestMethod: String?,
    val requestPath: String?,
    val statusCode: Int?,
    val detail: String?,
)

package com.manage.crm.support.web.idempotency

data class IdempotencyRecord(
    val method: String,
    val path: String,
    val key: String,
    val requestHash: String,
    val status: IdempotencyRecordStatus,
    val statusCode: Int? = null,
    val responseBody: String? = null,
    val contentType: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class IdempotencyRecordStatus {
    IN_PROGRESS,
    COMPLETED
}

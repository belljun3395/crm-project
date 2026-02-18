package com.manage.crm.support.web.idempotency

import java.security.MessageDigest

object IdempotencyKeyPolicy {
    const val HEADER_NAME = "Idempotency-Key"
    const val EXCHANGE_ATTRIBUTE = "idempotencyKey"

    private val keyRegex = Regex("^[A-Za-z0-9:_-]{8,128}$")

    fun isValid(key: String): Boolean = keyRegex.matches(key)

    fun hashRequestBody(body: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(body)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

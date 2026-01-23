package com.manage.crm.webhook.domain

enum class WebhookEventType(val value: String) {
    USER_CREATED("USER_CREATED"),
    EMAIL_SENT("EMAIL_SENT");

    companion object {
        fun fromValue(value: String): WebhookEventType {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported webhook event type: $value")
        }
    }
}

package com.manage.crm.webhook.domain

data class WebhookEvents(
    val value: List<WebhookEventType>
) {
    fun toValues(): List<String> = value.map { it.value }

    fun supports(eventType: WebhookEventType): Boolean = value.contains(eventType)

    companion object {
        fun fromValues(values: List<String>): WebhookEvents {
            return WebhookEvents(values.map { WebhookEventType.fromValue(it) })
        }
    }
}

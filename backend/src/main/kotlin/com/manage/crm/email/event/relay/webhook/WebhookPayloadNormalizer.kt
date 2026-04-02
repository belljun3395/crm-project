package com.manage.crm.email.event.relay.webhook

import com.manage.crm.email.event.relay.EmailTrackingEvent

interface WebhookPayloadNormalizer {
    fun normalize(payload: Map<String, Any?>): EmailTrackingEvent?
}

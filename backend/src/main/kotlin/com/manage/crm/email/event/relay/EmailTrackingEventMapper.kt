package com.manage.crm.email.event.relay

import com.manage.crm.email.event.send.EmailClickEvent
import com.manage.crm.email.event.send.EmailDeliveryDelayEvent
import com.manage.crm.email.event.send.EmailDeliveryEvent
import com.manage.crm.email.event.send.EmailOpenEvent
import com.manage.crm.email.event.send.EmailSendEvent
import org.springframework.stereotype.Component

@Component
class EmailTrackingEventMapper {
    fun toDomainEvent(event: EmailTrackingEvent): EmailSendEvent? =
        when (event.eventType) {
            EmailTrackingEventType.OPEN ->
                EmailOpenEvent(
                    messageId = event.messageId,
                    destination = event.destination,
                    timestamp = event.occurredAt,
                    provider = event.provider,
                )

            EmailTrackingEventType.DELIVERY ->
                EmailDeliveryEvent(
                    messageId = event.messageId,
                    destination = event.destination,
                    timestamp = event.occurredAt,
                    provider = event.provider,
                )

            EmailTrackingEventType.CLICK ->
                EmailClickEvent(
                    messageId = event.messageId,
                    destination = event.destination,
                    timestamp = event.occurredAt,
                    provider = event.provider,
                )

            EmailTrackingEventType.DELIVERY_DELAY ->
                EmailDeliveryDelayEvent(
                    messageId = event.messageId,
                    destination = event.destination,
                    timestamp = event.occurredAt,
                    provider = event.provider,
                )

            EmailTrackingEventType.SEND,
            EmailTrackingEventType.BOUNCE,
            EmailTrackingEventType.COMPLAINT,
            -> null
        }
}

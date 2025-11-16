package com.manage.crm.email.event.relay.aws

import com.manage.crm.email.domain.vo.EmailProviderType
import com.manage.crm.email.event.relay.aws.mapper.SesEmailNotification
import com.manage.crm.email.event.relay.aws.model.SesEventType
import com.manage.crm.email.event.send.EmailClickEvent
import com.manage.crm.email.event.send.EmailDeliveryDelayEvent
import com.manage.crm.email.event.send.EmailDeliveryEvent
import com.manage.crm.email.event.send.EmailOpenEvent
import com.manage.crm.email.event.send.EmailSendEvent
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class SesEmailEventFactory {

    fun toEmailSendEvent(notification: SesEmailNotification): Optional<EmailSendEvent> =
        when (notification.eventType) {
            SesEventType.OPEN ->
                Optional.of(
                    EmailOpenEvent(
                        messageId = notification.messageId,
                        destination = notification.destination,
                        timestamp = notification.occurredAt,
                        provider = EmailProviderType.AWS
                    )
                )

            SesEventType.DELIVERY ->
                Optional.of(
                    EmailDeliveryEvent(
                        messageId = notification.messageId,
                        destination = notification.destination,
                        timestamp = notification.occurredAt,
                        provider = EmailProviderType.AWS
                    )
                )

            SesEventType.CLICK ->
                Optional.of(
                    EmailClickEvent(
                        messageId = notification.messageId,
                        destination = notification.destination,
                        timestamp = notification.occurredAt,
                        provider = EmailProviderType.AWS
                    )
                )

            SesEventType.DELIVERY_DELAY ->
                Optional.of(
                    EmailDeliveryDelayEvent(
                        messageId = notification.messageId,
                        destination = notification.destination,
                        timestamp = notification.occurredAt,
                        provider = EmailProviderType.AWS
                    )
                )

            SesEventType.SEND,
            SesEventType.BOUNCE,
            SesEventType.COMPLAINT,
            SesEventType.REJECT,
            SesEventType.RENDERING_FAILURE -> Optional.empty()
        }
}

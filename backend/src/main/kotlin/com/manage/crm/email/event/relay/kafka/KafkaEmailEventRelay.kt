package com.manage.crm.email.event.relay.kafka

import com.manage.crm.email.event.relay.EmailTrackingEvent
import com.manage.crm.email.event.relay.EmailTrackingEventMapper
import com.manage.crm.email.support.EmailEventPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class KafkaEmailEventRelay(
    private val emailEventPublisher: EmailEventPublisher,
    private val emailTrackingEventMapper: EmailTrackingEventMapper,
) {
    private val log = KotlinLogging.logger {}

    @KafkaListener(
        topics = ["\${email.tracking.kafka.topic:email-tracking-events}"],
        groupId = "\${email.tracking.kafka.group-id:crm-email-tracking-consumer}",
        containerFactory = "emailTrackingKafkaListenerContainerFactory",
    )
    fun onMessage(
        event: EmailTrackingEvent,
        acknowledgment: Acknowledgment,
    ) {
        runCatching {
            emailTrackingEventMapper.toDomainEvent(event)?.let { domainEvent ->
                emailEventPublisher.publishEvent(domainEvent)
                log.info { "Published email tracking domain event: ${domainEvent::class.simpleName} for messageId=${event.messageId}" }
            }
        }.onFailure { e ->
            log.error(e) { "Failed to process email tracking event: $event" }
        }
        acknowledgment.acknowledge()
    }
}

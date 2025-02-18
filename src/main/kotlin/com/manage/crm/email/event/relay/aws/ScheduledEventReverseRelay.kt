package com.manage.crm.email.event.relay.aws

import com.manage.crm.email.event.relay.aws.mapper.ScheduledEventMessageMapper
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutInvokeEvent
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!local && !test")
@Component
class ScheduledEventReverseRelay(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val scheduledEventMessageMapper: ScheduledEventMessageMapper
) {
    val log = KotlinLogging.logger { }

    @SqsListener(queueNames = ["crm_schedule_event_sqs"])
    fun onMessage(
        message: String,
        acknowledgement: Acknowledgement
    ) {
        scheduledEventMessageMapper.map(message)
            .let { scheduledEventMessageMapper.toEvent(it) }
            .let { publish(it) }
        acknowledgement.acknowledge()
    }

    fun publish(event: NotificationEmailSendTimeOutInvokeEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}

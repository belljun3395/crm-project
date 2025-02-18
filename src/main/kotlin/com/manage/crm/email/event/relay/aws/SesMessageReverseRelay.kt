package com.manage.crm.email.event.relay.aws

import com.manage.crm.email.event.relay.aws.mapper.SesMessageMapper
import com.manage.crm.email.event.send.EmailSendEvent
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!local && !test")
@Component
class SesMessageReverseRelay(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val eventMessageMapper: SesMessageMapper
) {
    val log = KotlinLogging.logger { }

    @SqsListener(queueNames = ["crm_ses_sqs"])
    fun onMessage(
        message: String,
        acknowledgement: Acknowledgement
    ) {
        eventMessageMapper.map(message)
            .let { eventMessageMapper.toEvent(it) }
            .ifPresent { publish(it) }

        acknowledgement.acknowledge()
    }

    fun publish(event: EmailSendEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}

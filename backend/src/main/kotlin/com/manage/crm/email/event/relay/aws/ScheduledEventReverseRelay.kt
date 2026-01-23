package com.manage.crm.email.event.relay.aws

import com.manage.crm.email.event.relay.aws.mapper.ScheduledEventMessageMapper
import com.manage.crm.infrastructure.scheduler.handler.ScheduledTaskHandler
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!local && !test && !local-dev")
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "aws")
class ScheduledEventReverseRelay(
    private val scheduledEventMessageMapper: ScheduledEventMessageMapper,
    private val scheduledTaskHandler: ScheduledTaskHandler
) {
    val log = KotlinLogging.logger { }

    @SqsListener(queueNames = ["crm_schedule_event_sqs"])
    fun onMessage(
        message: String,
        acknowledgement: Acknowledgement
    ) {
        scheduledEventMessageMapper.map(message)
            .let { scheduledEventMessageMapper.toInput(it) }
            .let { scheduledTaskHandler.handle(it) }
        acknowledgement.acknowledge()
    }
}

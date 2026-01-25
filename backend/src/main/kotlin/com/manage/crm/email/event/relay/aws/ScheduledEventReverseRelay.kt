package com.manage.crm.email.event.relay.aws

import com.manage.crm.email.event.relay.aws.mapper.ScheduledEventMessageMapper
import com.manage.crm.email.event.schedule.handler.ScheduledTaskHandler
import io.awspring.cloud.sqs.annotation.SqsListener
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!local && !test && !local-dev")
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "aws")
@Component
class ScheduledEventReverseRelay(
    private val scheduledTaskHandler: ScheduledTaskHandler,
    private val scheduledEventMessageMapper: ScheduledEventMessageMapper
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

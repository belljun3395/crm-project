package com.manage.crm.email.event.relay.aws.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutInvokeEvent
import org.springframework.stereotype.Component

data class ScheduledEventMessage(
    val campaignId: Long?,
    val templateId: Long,
    val templateVersion: Float?,
    val userIds: List<Long>,
    val segmentId: Long?,
    val eventId: String,
)

@Component
class ScheduledEventMessageMapper(
    private val objectMapper: ObjectMapper,
) {
    fun map(message: String): ScheduledEventMessage {
        objectMapper.readTree(message).let { jsonNode ->
            val templateId = jsonNode["templateId"].asLong()
            val templateVersion = jsonNode["templateVersion"]?.asDouble()?.toFloat()?.let { if (it == 0.0f) null else it }
            val userIds = jsonNode["userIds"].map { it.asLong() }
            val segmentId = jsonNode["segmentId"]?.asLong()
            val campaignId = jsonNode["campaignId"]?.asLong()
            val eventId = jsonNode["eventId"].asText()

            return ScheduledEventMessage(
                campaignId = campaignId,
                templateId = templateId,
                templateVersion = templateVersion,
                userIds = userIds,
                segmentId = segmentId,
                eventId = eventId,
            )
        }
    }

    fun toEvent(message: ScheduledEventMessage): NotificationEmailSendTimeOutInvokeEvent =
        NotificationEmailSendTimeOutInvokeEvent(
            timeOutEventId = EventId(message.eventId),
            campaignId = message.campaignId,
            templateId = message.templateId,
            templateVersion = message.templateVersion,
            userIds = message.userIds,
            segmentId = message.segmentId,
        )
}

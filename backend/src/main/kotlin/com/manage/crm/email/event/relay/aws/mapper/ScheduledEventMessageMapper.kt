package com.manage.crm.email.event.relay.aws.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import org.springframework.stereotype.Component
import java.time.LocalDateTime

data class ScheduledEventMessage(
    val templateId: Long,
    val templateVersion: Float?,
    val userIds: List<Long>,
    val eventId: String
)

@Component
class ScheduledEventMessageMapper(
    private val objectMapper: ObjectMapper
) {
    fun map(message: String): ScheduledEventMessage {
        objectMapper.readTree(message).let { jsonNode ->
            val templateId = jsonNode["templateId"].asLong()
            val templateVersion = jsonNode["templateVersion"]?.asDouble()?.toFloat()?.let { if (it == 0.0f) null else it }
            val userIds = jsonNode["userIds"].map { it.asLong() }
            val eventId = jsonNode["eventId"].asText()

            return ScheduledEventMessage(
                templateId = templateId,
                templateVersion = templateVersion,
                userIds = userIds,
                eventId = eventId
            )
        }
    }

    fun toInput(message: ScheduledEventMessage): NotificationEmailSendTimeOutEventInput {
        return NotificationEmailSendTimeOutEventInput(
            eventId = EventId(message.eventId),
            templateId = message.templateId,
            templateVersion = message.templateVersion,
            userIds = message.userIds,
            expiredTime = LocalDateTime.now()
        )
    }
}

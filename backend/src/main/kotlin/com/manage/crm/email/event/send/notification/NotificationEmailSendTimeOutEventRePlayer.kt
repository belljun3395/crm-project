package com.manage.crm.email.event.send.notification

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.service.ScheduleTaskAllService
import com.manage.crm.email.domain.repository.ScheduledEventRepository
import com.manage.crm.support.LocalDateTimeExtension
import com.manage.crm.support.parseExpiredTime
import com.manage.crm.support.transactional.TransactionTemplates
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

fun JsonNode.campaignId(): Long? {
    val node = this.get("campaignId") ?: return null
    return when {
        node.isIntegralNumber -> node.longValue()
        node.isTextual -> node.asText().toLongOrNull()
        else -> null
    }
}

fun JsonNode.templateId(): Long {
    val node = this.get("templateId") ?: throw IllegalArgumentException("templateId is required")
    return when {
        node.isIntegralNumber -> node.longValue()
        node.isTextual -> node.asText().toLongOrNull() ?: throw IllegalArgumentException("Invalid templateId format")
        else -> throw IllegalArgumentException("Invalid templateId type")
    }
}

fun JsonNode.templateVersion(): Float {
    val node = this.get("templateVersion") ?: throw IllegalArgumentException("templateVersion is required")
    return when {
        node.isNumber -> node.floatValue()
        node.isTextual -> node.asText().toFloatOrNull() ?: throw IllegalArgumentException("Invalid templateVersion format")
        else -> throw IllegalArgumentException("Invalid templateVersion type")
    }
}

fun JsonNode.userIds(): List<Long> {
    val node = this.get("userIds") ?: throw IllegalArgumentException("userIds is required")
    return if (node.isArray) {
        node.mapNotNull { userIdNode ->
            when {
                userIdNode.isIntegralNumber -> userIdNode.longValue()
                userIdNode.isTextual -> userIdNode.asText().toLongOrNull()
                else -> null
            }
        }
    } else {
        throw IllegalArgumentException("userIds must be an array")
    }
}

fun JsonNode.expiredTime() = LocalDateTimeExtension().parseExpiredTime(
    this.get("expiredTime")?.asText() ?: throw IllegalArgumentException("expiredTime is required")
)

@Profile("!test")
@Component
class NotificationEmailSendTimeOutEventRePlayer(
    private val eventScheduleRepository: ScheduledEventRepository,
    private val objectMapper: ObjectMapper,
    private val transactionalTemplates: TransactionTemplates,
    @Qualifier("scheduleTaskServicePostEventProcessor")
    private val scheduleTaskService: ScheduleTaskAllService
) : ApplicationRunner {
    val log = KotlinLogging.logger {}

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun run(args: ApplicationArguments) {
        scope.launch {
            transactionalTemplates.newTxWriter.executeAndAwait {
                replay { expiredEventsLogBuffer, replayedEventsLogBuffer ->
                    var expiredEventCount = 0L
                    var replayedEventCount = 0L
                    eventScheduleRepository.findAllByEventClassAndCompletedFalse(
                        NotificationEmailSendTimeOutEvent::class.simpleName.toString()
                    )
                        .forEach {
                            objectMapper.readTree(it.eventPayload).let { payload ->
                                val event =
                                    NotificationEmailSendTimeOutEvent(
                                        campaignId = payload.campaignId(),
                                        eventId = it.eventId,
                                        templateId = payload.templateId(),
                                        templateVersion = payload.templateVersion(),
                                        userIds = payload.userIds(),
                                        expiredTime = payload.expiredTime()
                                    )
                                if (event.isExpired()) {
                                    expiredEventsLogBuffer.appendLine("  - eventId: ${event.eventId} expiredTime: ${event.expiredTime}")
                                    eventScheduleRepository.findByEventId(event.eventId)
                                        ?.notConsumed()?.complete()
                                    expiredEventCount++
                                } else {
                                    replayedEventsLogBuffer.appendLine("  - eventId: ${event.eventId} expiredTime: ${event.expiredTime}")
                                    val notificationEmailSendTimeOutEventInput =
                                        NotificationEmailSendTimeOutEventInput(
                                            campaignId = event.campaignId,
                                            templateId = event.templateId,
                                            templateVersion = event.templateVersion,
                                            userIds = event.userIds,
                                            eventId = event.eventId,
                                            expiredTime = event.expiredTime
                                        )
                                    scheduleTaskService.reSchedule(
                                        notificationEmailSendTimeOutEventInput
                                    )
                                    replayedEventCount++
                                }
                            }
                        }

                    return@replay listOf(expiredEventCount, replayedEventCount)
                }
            }
        }
    }

    suspend fun replay(logic: suspend (expiredEventsLogBuffer: StringBuilder, replayedEventsLogBuffer: StringBuilder) -> List<Long>) {
        val logBuffer = StringBuilder()
        logBuffer.appendLine()
        logBuffer.appendLine("----------------- NotificationEmailSendTimeOutEventReplay -----------------")
        val expiredEventsLogBuffer =
            StringBuilder().apply {
                appendLine("Expired events:")
            }
        val replayedEventsLogBuffer =
            StringBuilder().apply {
                appendLine("Replayed events:")
            }

        val (expiredEventCount, replayedEventCount) = logic(
            expiredEventsLogBuffer,
            replayedEventsLogBuffer
        )

        logBuffer.append(expiredEventsLogBuffer)
        logBuffer.append(replayedEventsLogBuffer)
        logBuffer.appendLine("Expired event count: $expiredEventCount")
        logBuffer.append("Replayed event count: $replayedEventCount")
        logBuffer.appendLine()
        logBuffer.appendLine("-----------------------------------------------------------------------------")
        log.info { logBuffer.toString() }
    }
}

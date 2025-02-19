package com.manage.crm.email.event.send.notification

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.service.ScheduleTaskService
import com.manage.crm.email.domain.repository.ScheduledEventRepository
import com.manage.crm.support.LocalDateTimeExtension
import com.manage.crm.support.parseExpiredTime
import com.manage.crm.support.transactional.TransactionTemplates
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

fun JsonNode.templateId() = this["templateId"].asLong()

fun JsonNode.templateVersion() = (this["templateVersion"].asDouble()).toFloat()

fun JsonNode.userIds() = this["userIds"].map { it.asLong() }

fun JsonNode.expiredTime() = LocalDateTimeExtension().parseExpiredTime(this["expiredTime"].asText())

@Component
class NotificationEmailSendTimeOutEventRePlayer(
    private val eventScheduleRepository: ScheduledEventRepository,
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val transactionalTemplates: TransactionTemplates,
    private val scheduleTaskService: ScheduleTaskService
) : ApplicationRunner {
    val log = KotlinLogging.logger {}

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun run(args: ApplicationArguments) {
        scope.launch {
            transactionalTemplates.newTxWriter.executeAndAwait {
                replay { expiredEventsLogBuffer, replayedEventsLogBuffer ->
                    var expiredEventCount = 0L
                    var replayedEventCount = 0L
                    eventScheduleRepository.findAllByEventClassAndCompletedFalse(NotificationEmailSendTimeOutEvent::class.simpleName.toString())
                        .forEach {
                            objectMapper.readTree(it.eventPayload).let { payload ->
                                val event =
                                    NotificationEmailSendTimeOutEvent(
                                        eventId = it.eventId!!,
                                        templateId = payload.templateId(),
                                        templateVersion = payload.templateVersion(),
                                        userIds = payload.userIds(),
                                        expiredTime = payload.expiredTime()
                                    )
                                if (event.isExpired()) {
                                    expiredEventsLogBuffer.appendLine("  - eventId: ${event.eventId} expiredTime: ${event.expiredTime}")
                                    eventScheduleRepository.findByEventId(event.eventId)?.notConsumed()?.complete()
                                    expiredEventCount++
                                } else {
                                    replayedEventsLogBuffer.appendLine("  - eventId: ${event.eventId} expiredTime: ${event.expiredTime}")
                                    scheduleTaskService.reSchedule(
                                        NotificationEmailSendTimeOutEventInput(
                                            templateId = event.templateId,
                                            templateVersion = event.templateVersion,
                                            userIds = event.userIds,
                                            eventId = event.eventId,
                                            expiredTime = event.expiredTime
                                        )
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

        val (expiredEventCount, replayedEventCount) = logic(expiredEventsLogBuffer, replayedEventsLogBuffer)

        logBuffer.append(expiredEventsLogBuffer)
        logBuffer.append(replayedEventsLogBuffer)
        logBuffer.appendLine("Expired event count: $expiredEventCount")
        logBuffer.append("Replayed event count: $replayedEventCount")
        logBuffer.appendLine()
        logBuffer.appendLine("-----------------------------------------------------------------------------")
        log.info { logBuffer.toString() }
    }
}

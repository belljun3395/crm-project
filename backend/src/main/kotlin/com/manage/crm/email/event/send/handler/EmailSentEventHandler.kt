package com.manage.crm.email.event.send.handler

import com.manage.crm.email.domain.EmailSendHistory
import com.manage.crm.email.domain.repository.EmailSendHistoryRepository
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.email.event.send.EmailSentEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class EmailSentEventHandler(
    private val emailSendHistoryRepository: EmailSendHistoryRepository
) {
    val log = KotlinLogging.logger {}

    /**
     * - Save the email send history
     */
    suspend fun handle(event: EmailSentEvent) {
        emailSendHistoryRepository.save(
            EmailSendHistory.new(
                userId = event.userId,
                userEmail = event.destination,
                emailMessageId = event.messageId,
                emailBody = event.emailBody,
                sendStatus = SentEmailStatus.SEND.name
            )
        )
    }
}

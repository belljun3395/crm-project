package com.manage.crm.email.event.send.handler

import com.manage.crm.email.domain.EmailSendHistory
import com.manage.crm.email.domain.repository.EmailSendHistoryRepository
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.email.event.send.EmailSentEvent
import com.manage.crm.support.transactional.TransactionTemplates
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

@Component
class EmailSentEventHandler(
    private val emailSendHistoryRepository: EmailSendHistoryRepository,
    private val transactionalTemplates: TransactionTemplates
) {
    val log = KotlinLogging.logger {}

    /**
     * - Save the email send history
     */
    suspend fun handle(event: EmailSentEvent) {
        transactionalTemplates.writer.executeAndAwait {
            emailSendHistoryRepository.save(
                EmailSendHistory(
                    userId = event.userId,
                    userEmail = event.destination,
                    emailMessageId = event.messageId,
                    emailBody = event.emailBody,
                    sendStatus = SentEmailStatus.SEND.name
                )
            )
        }
    }
}

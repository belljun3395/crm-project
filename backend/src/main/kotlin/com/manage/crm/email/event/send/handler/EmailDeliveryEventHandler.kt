package com.manage.crm.email.event.send.handler

import com.manage.crm.email.domain.repository.EmailSendHistoryRepository
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.email.event.send.EmailDeliveryEvent
import com.manage.crm.support.transactional.TransactionTemplates
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

@Component
class EmailDeliveryEventHandler(
    private val emailSendHistoryRepository: EmailSendHistoryRepository,
    private val transactionalTemplates: TransactionTemplates
) {
    val log = KotlinLogging.logger {}

    /**
     *  - Update the status of the email to "DELIVERY"
     */
    suspend fun handle(event: EmailDeliveryEvent) {
        transactionalTemplates.writer.executeAndAwait {
            emailSendHistoryRepository
                .findByEmailMessageId(event.messageId)
                ?.let {
                    it.sendStatus = SentEmailStatus.DELIVERY.name
                    emailSendHistoryRepository.save(it)
                }
                ?: log.error { "EmailSendHistory not found by email messageId: ${event.messageId}" }
        }
    }
}

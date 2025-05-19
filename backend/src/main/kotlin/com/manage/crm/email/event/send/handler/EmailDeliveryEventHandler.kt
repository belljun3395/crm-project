package com.manage.crm.email.event.send.handler

import com.manage.crm.email.domain.repository.EmailSendHistoryRepository
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.email.event.send.EmailDeliveryEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class EmailDeliveryEventHandler(
    private val emailSendHistoryRepository: EmailSendHistoryRepository
) {
    val log = KotlinLogging.logger {}

    /**
     *  - Update the status of the email to "DELIVERY"
     */
    suspend fun handle(event: EmailDeliveryEvent) {
        emailSendHistoryRepository
            .findByEmailMessageId(event.messageId)
            ?.let {
                it.sendStatus = SentEmailStatus.DELIVERY.name
                emailSendHistoryRepository.save(it)
            }
            ?: log.error { "EmailSendHistory not found by email messageId: ${event.messageId}" }
    }
}

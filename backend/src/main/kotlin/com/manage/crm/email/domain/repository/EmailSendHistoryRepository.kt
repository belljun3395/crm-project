package com.manage.crm.email.domain.repository

import com.manage.crm.email.domain.EmailSendHistory
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface EmailSendHistoryRepository : CoroutineCrudRepository<EmailSendHistory, Long> {
    suspend fun findByEmailMessageId(emailMessageId: String): EmailSendHistory?
}

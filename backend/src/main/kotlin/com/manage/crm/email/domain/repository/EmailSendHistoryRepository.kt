package com.manage.crm.email.domain.repository

import com.manage.crm.email.domain.EmailSendHistory
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface EmailSendHistoryRepository : CoroutineCrudRepository<EmailSendHistory, Long> {
    suspend fun findByEmailMessageId(emailMessageId: String): EmailSendHistory?
    fun findByUserId(userId: Long): Flow<EmailSendHistory>
    fun findBySendStatus(sendStatus: String): Flow<EmailSendHistory>
    fun findByUserIdAndSendStatus(userId: Long, sendStatus: String): Flow<EmailSendHistory>
    fun findAllByOrderByCreatedAtDesc(): Flow<EmailSendHistory>
    suspend fun countByUserId(userId: Long): Long
    suspend fun countBySendStatus(sendStatus: String): Long
    suspend fun countByUserIdAndSendStatus(userId: Long, sendStatus: String): Long
}

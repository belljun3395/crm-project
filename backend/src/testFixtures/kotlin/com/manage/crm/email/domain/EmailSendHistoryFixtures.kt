package com.manage.crm.email.domain

import com.manage.crm.email.domain.vo.Email
import com.manage.crm.email.domain.vo.EmailFixtures
import com.manage.crm.email.domain.vo.SentEmailStatus
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class EmailSendHistoryFixtures private constructor() {
    private var id: Long = -1L
    private var userId: Long = -1L
    private var userEmail: Email = EmailFixtures.giveMeOne().build()
    private var emailMessageId: String = UUID.randomUUID().toString()
    private var emailBody: String = "Default email body"
    private var sendStatus: String = SentEmailStatus.SEND.name
    private var createdAt: LocalDateTime = LocalDateTime.now()
    private var updatedAt: LocalDateTime = LocalDateTime.now()

    fun withId(id: Long) = apply { this.id = id }
    fun withUserId(userId: Long) = apply { this.userId = userId }
    fun withUserEmail(userEmail: Email) = apply { this.userEmail = userEmail }
    fun withEmailMessageId(emailMessageId: String) = apply { this.emailMessageId = emailMessageId }
    fun withEmailBody(emailBody: String) = apply { this.emailBody = emailBody }
    fun withSendStatus(sendStatus: String) = apply { this.sendStatus = sendStatus }
    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }
    fun withUpdatedAt(updatedAt: LocalDateTime) = apply { this.updatedAt = updatedAt }

    fun build() = EmailSendHistory(
        id = id,
        userId = userId,
        userEmail = userEmail,
        emailMessageId = emailMessageId,
        emailBody = emailBody,
        sendStatus = sendStatus,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun anEmailSendHistory() = EmailSendHistoryFixtures()

        fun giveMeOne(): EmailSendHistoryFixtures {
            val id = Random.nextLong(1, 101)
            val userId = Random.nextLong(1, 101)
            val userEmail = EmailFixtures.giveMeOne().build()
            val emailMessageId = UUID.randomUUID().toString()
            val emailBody = "This is a test email body."
            val sendStatus = SentEmailStatus.SEND.name

            return anEmailSendHistory()
                .withId(id)
                .withUserId(userId)
                .withUserEmail(userEmail)
                .withEmailMessageId(emailMessageId)
                .withEmailBody(emailBody)
                .withSendStatus(sendStatus)
        }
    }
}

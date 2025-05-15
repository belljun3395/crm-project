package com.manage.crm.email.domain

import com.manage.crm.email.domain.vo.Email
import com.manage.crm.email.domain.vo.SentEmailStatus
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("email_send_histories")
class EmailSendHistory(
    @Id
    var id: Long? = null,
    @Column("user_id")
    var userId: Long,
    @Column("user_email")
    var userEmail: Email,
    @Column("email_message_id")
    var emailMessageId: String,
    @Column("email_body")
    var emailBody: String,
    @Column("send_status")
    var sendStatus: String,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            userId: Long,
            userEmail: String,
            emailMessageId: String,
            emailBody: String,
            sendStatus: String
        ): EmailSendHistory {
            return this.new(userId, Email(userEmail), emailMessageId, emailBody, sendStatus)
        }

        fun new(
            userId: Long,
            userEmail: Email,
            emailMessageId: String,
            emailBody: String,
            sendStatus: String
        ): EmailSendHistory {
            require(SentEmailStatus.contains(sendStatus)) { "Invalid send status: $sendStatus" }
            return EmailSendHistory(
                userId = userId,
                userEmail = userEmail,
                emailMessageId = emailMessageId,
                emailBody = emailBody,
                sendStatus = sendStatus
            )
        }
    }
}

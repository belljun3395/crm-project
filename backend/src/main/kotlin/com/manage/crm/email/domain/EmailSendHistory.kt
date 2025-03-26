package com.manage.crm.email.domain

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
    var userId: Long? = null,
    @Column("user_email")
    var userEmail: String? = null,
    @Column("email_message_id")
    var emailMessageId: String? = null,
    @Column("email_body")
    var emailBody: String? = null,
    @Column("send_status")
    var sendStatus: String? = null,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null
)

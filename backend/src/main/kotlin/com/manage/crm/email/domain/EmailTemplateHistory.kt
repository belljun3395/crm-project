package com.manage.crm.email.domain

import com.manage.crm.email.domain.vo.Variables
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

// TODO: templateId 와 version을 기준으로 유니크 해야한다.
@Table("email_template_histories")
class EmailTemplateHistory(
    @Id
    var id: Long? = null,
    @Column("template_id")
    var templateId: Long? = null,
    @Column("subject")
    var subject: String? = null,
    @Column("body")
    var body: String? = null,
    @Column("variables")
    var variables: Variables = Variables(),
    @Column("version")
    var version: Float? = null,
    @CreatedDate
    var createdAt: LocalDateTime? = null
) {
    companion object {
        private const val INITIAL_VERSION_AMOUNT = 1.0f

        fun new(
            templateId: Long,
            subject: String,
            body: String,
            variables: Variables,
            version: Float
        ): EmailTemplateHistory {
            return EmailTemplateHistory(
                templateId = templateId,
                subject = subject,
                body = body,
                variables = variables,
                version = version
            )
        }

        fun new(
            id: Long,
            templateId: Long,
            subject: String,
            body: String,
            variables: Variables,
            version: Float,
            createdAt: LocalDateTime
        ): EmailTemplateHistory {
            return EmailTemplateHistory(
                id = id,
                templateId = templateId,
                subject = subject,
                body = body,
                variables = variables,
                version = version,
                createdAt = createdAt
            )
        }
    }
}

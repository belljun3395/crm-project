package com.manage.crm.email.domain

import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.email.event.template.PostEmailTemplateEvent
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("email_templates")
class EmailTemplate(
    @Id
    var id: Long? = null,
    @Column("template_name")
    var templateName: String? = null,
    @Column("subject")
    var subject: String? = null,
    @Column("body")
    var body: String? = null,
    @Column("variables")
    var variables: Variables = Variables(),
    @Column("version")
    var version: Float = 1.0f,
    @CreatedDate
    var createdAt: LocalDateTime? = null
) {
    @Transient
    var domainEvents: MutableList<PostEmailTemplateEvent> = mutableListOf()

    companion object {
        private const val DEFAULT_VERSION_PLUS_AMOUNT = 0.1f

        fun new(templateName: String, subject: String, body: String, variables: Variables): EmailTemplate {
            return EmailTemplate(
                templateName = templateName,
                subject = subject,
                body = body,
                variables = variables
            )
        }

        fun new(
            id: Long,
            templateName: String,
            subject: String,
            body: String,
            variables: Variables,
            version: Float,
            createdAt: LocalDateTime
        ): EmailTemplate {
            return EmailTemplate(
                id = id,
                templateName = templateName,
                subject = subject,
                body = body,
                variables = variables,
                version = version,
                createdAt = createdAt
            )
        }
    }

    /**
     * Check if the template is new.
     */
    fun isNewTemplate(): Boolean = id == null

    // ----------------- Modify Builder -----------------
    /**
     * Modify the email template.
     */
    fun modify(): EmailTemplateModifyBuilder = EmailTemplateModifyBuilder(this)

    class EmailTemplateModifyBuilder(
        private val template: EmailTemplate,
        private var isVersionUpdated: Boolean = false
    ) {
        /**
         * Finalize the modification and return the modified template.
         */
        fun done(): EmailTemplate {
            if (!isVersionUpdated) {
                updateVersion(null)
            }
            template.registerModifyEvent()
            return template
        }

        /**
         * Modify the subject of the email template.
         */
        fun modifySubject(subject: String?): EmailTemplateModifyBuilder {
            subject?.let {
                template.subject = subject
            }
            return this
        }

        /**
         * Modify the body of the email template.
         */
        fun modifyBody(
            body: String,
            variables: Variables
        ): EmailTemplateModifyBuilder {
            template.body = body
            template.variables = variables
            return this
        }

        /**
         * Update the version of the email template.
         */
        fun updateVersion(version: Float?): EmailTemplateModifyBuilder {
            version?.let {
                if (it <= template.version) {
                    throw IllegalArgumentException("Invalid version: $it")
                }
                this.template.version = it
            } ?: kotlin.run {
                this.template.version += DEFAULT_VERSION_PLUS_AMOUNT
            }
            isVersionUpdated = true
            return this
        }
    }

    /**
     * Add Post Email Template Event to the domain events list.
     */
    private fun registerModifyEvent() {
        domainEvents.add(
            PostEmailTemplateEvent(
                templateId = this.id!!
            )
        )
    }
}

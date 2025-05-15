package com.manage.crm.email.domain

import com.manage.crm.email.domain.vo.EmailTemplateVersion
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
    var templateName: String,
    @Column("subject")
    var subject: String,
    @Column("body")
    var body: String,
    @Column("variables")
    var variables: Variables,
    @Column("version")
    var version: EmailTemplateVersion,
    @CreatedDate
    var createdAt: LocalDateTime? = null
) {
    @Transient
    var domainEvents: MutableList<PostEmailTemplateEvent> = mutableListOf()

    companion object {

        fun new(templateName: String, subject: String, body: String, variables: Variables): EmailTemplate {
            return EmailTemplate(
                templateName = templateName,
                subject = subject,
                body = body,
                variables = variables,
                version = EmailTemplateVersion()
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
                version = EmailTemplateVersion(version),
                createdAt = createdAt
            )
        }

        fun new(
            id: Long,
            templateName: String,
            subject: String,
            body: String,
            variables: Variables,
            version: EmailTemplateVersion,
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
            val currentVersion = requireNotNull(this.template.version) { "Version must not be null" }
            version?.let {
                if (!EmailTemplateVersion.isValidUpdateVersion(currentVersion, it)) {
                    throw IllegalArgumentException("Invalid version: $it")
                }
                this.template.version = EmailTemplateVersion(it)
            } ?: kotlin.run {
                this.template.version =
                    EmailTemplateVersion(EmailTemplateVersion.calcNewVersion(currentVersion))
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

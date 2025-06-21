package com.manage.crm.email.domain

import com.manage.crm.email.domain.vo.EmailTemplateVersion
import com.manage.crm.email.domain.vo.EmailTemplateVersionFixtures
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.email.domain.vo.VariablesFixtures
import java.time.LocalDateTime
import kotlin.random.Random

class EmailTemplateFixtures private constructor() {
    private var id: Long = -1L
    private var templateName: String = "default-template-name"
    private var subject: String = "Default Subject"
    private var body: String = "<p>Default Body</p>"
    private var variables: Variables = VariablesFixtures.aVariables().build()
    private var version: EmailTemplateVersion = EmailTemplateVersionFixtures.anEmailTemplateVersion().build()
    private var createdAt: LocalDateTime = LocalDateTime.now()

    fun withId(id: Long) = apply { this.id = id }
    fun withTemplateName(templateName: String) = apply { this.templateName = templateName }
    fun withSubject(subject: String) = apply { this.subject = subject }
    fun withBody(body: String) = apply { this.body = body }
    fun withVariables(variables: Variables) = apply { this.variables = variables }
    fun withVersion(version: EmailTemplateVersion) = apply { this.version = version }
    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun build(): EmailTemplate = EmailTemplate(
        id = id,
        templateName = templateName,
        subject = subject,
        body = body,
        variables = variables,
        version = version,
        createdAt = createdAt
    )

    companion object {
        fun anEmailTemplate() = EmailTemplateFixtures()

        fun giveMeOne(): EmailTemplateFixtures {
            val randomSuffix = Random.nextInt(1000)
            val id = Random.nextLong(1, 101)
            val templateName = "template-$randomSuffix"
            val subject = "Subject $randomSuffix"
            val body = "Body for template $randomSuffix"
            val variables = VariablesFixtures.giveMeOne().build()
            val version = EmailTemplateVersionFixtures.giveMeOne().build()
            return anEmailTemplate()
                .withId(id)
                .withTemplateName(templateName)
                .withSubject(subject)
                .withBody(body)
                .withVariables(variables)
                .withVersion(version)
        }
    }
}

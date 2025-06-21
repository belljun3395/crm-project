package com.manage.crm.email.domain

import com.manage.crm.email.domain.vo.EmailTemplateVersion
import com.manage.crm.email.domain.vo.EmailTemplateVersionFixtures
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.email.domain.vo.VariablesFixtures
import java.time.LocalDateTime
import kotlin.random.Random

class EmailTemplateHistoryFixtures private constructor() {
    private var id: Long = -1L
    private var templateId: Long = 0
    private var subject: String = "Default History Subject"
    private var body: String = "<p>Default History Body</p>"
    private var variables: Variables = VariablesFixtures.aVariables().build()
    private var version: EmailTemplateVersion = EmailTemplateVersionFixtures.anEmailTemplateVersion().build()
    private var createdAt: LocalDateTime = LocalDateTime.now()

    fun withId(id: Long) = apply { this.id = id }
    fun withTemplateId(templateId: Long) = apply { this.templateId = templateId }
    fun withSubject(subject: String) = apply { this.subject = subject }
    fun withBody(body: String) = apply { this.body = body }
    fun withVariables(variables: Variables) = apply { this.variables = variables }
    fun withVersion(version: EmailTemplateVersion) = apply { this.version = version }
    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun build() = EmailTemplateHistory(
        id = id,
        templateId = templateId,
        subject = subject,
        body = body,
        variables = variables,
        version = version,
        createdAt = createdAt
    )

    companion object {
        fun anEmailTemplateHistory() = EmailTemplateHistoryFixtures()

        fun giveMeOne(): EmailTemplateHistoryFixtures {
            val id = Random.nextLong(1, 101)
            val templateId = Random.nextLong(1, 101)
            val subject = "History Subject ${Random.nextInt(100)}"
            val body = "History Body ${Random.nextInt(100)}"
            val variables = VariablesFixtures.giveMeOne().build()
            val version = EmailTemplateVersionFixtures.giveMeOne().build()

            return anEmailTemplateHistory()
                .withId(id)
                .withTemplateId(templateId)
                .withSubject(subject)
                .withBody(body)
                .withVariables(variables)
                .withVersion(version)
        }
    }
}

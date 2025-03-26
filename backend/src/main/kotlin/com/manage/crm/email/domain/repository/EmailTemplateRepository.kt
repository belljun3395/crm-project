package com.manage.crm.email.domain.repository

import com.manage.crm.email.domain.EmailTemplate
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface EmailTemplateRepository : CoroutineCrudRepository<EmailTemplate, Long> {
    suspend fun findByTemplateName(templateName: String): EmailTemplate?
}

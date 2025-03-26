package com.manage.crm.email.domain.repository

import com.manage.crm.email.domain.EmailTemplateHistory
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface EmailTemplateHistoryRepository : CoroutineCrudRepository<EmailTemplateHistory, Long> {
    suspend fun findAllByTemplateIdInOrderByVersionDesc(templateIds: List<Long>): List<EmailTemplateHistory>

    suspend fun findByTemplateIdAndVersion(
        templateId: Long,
        version: Float
    ): EmailTemplateHistory?
}

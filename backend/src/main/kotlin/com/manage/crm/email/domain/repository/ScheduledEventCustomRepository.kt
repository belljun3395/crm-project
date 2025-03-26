package com.manage.crm.email.domain.repository

import com.manage.crm.email.domain.ScheduledEvent

interface ScheduledEventCustomRepository {
    suspend fun findAllByEmailTemplateIdAndCompletedFalse(templateId: Long): List<ScheduledEvent>
}

package com.manage.crm.email.domain.repository

import com.manage.crm.email.domain.ScheduledEvent
import com.manage.crm.email.domain.vo.EventId

interface ScheduledEventCustomRepository {
    suspend fun findAllByEmailTemplateIdAndCompletedFalse(templateId: Long): List<ScheduledEvent>

    suspend fun findByEventIdAndCompletedFalseForUpdate(eventId: EventId): ScheduledEvent?
}

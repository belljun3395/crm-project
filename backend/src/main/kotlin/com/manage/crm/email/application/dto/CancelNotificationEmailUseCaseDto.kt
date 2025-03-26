package com.manage.crm.email.application.dto

import com.manage.crm.email.domain.vo.EventId

data class CancelNotificationEmailUseCaseIn(
    val eventId: EventId
)

data class CancelNotificationEmailUseCaseOut(
    val success: Boolean
)
class CancelNotificationEmailUseCaseDto

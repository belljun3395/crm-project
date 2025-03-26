package com.manage.crm.email.application.dto

data class SendNotificationEmailUseCaseIn(
    val templateId: Long,
    val templateVersion: Float?,
    val userIds: List<Long>
)

class SendNotificationEmailUseCaseOut(
    val isSuccess: Boolean
)

class SendNotificationEmailUseCaseDto

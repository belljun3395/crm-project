package com.manage.crm.email.application.dto

data class SendNotificationEmailUseCaseIn(
    val campaignId: Long?,
    val templateId: Long,
    val templateVersion: Float?,
    val userIds: List<Long>
)

class SendNotificationEmailUseCaseOut(
    val isSuccess: Boolean
)

class SendNotificationEmailUseCaseDto

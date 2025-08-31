package com.manage.crm.email.controller.request

data class SendNotificationEmailRequest(
    val campaignId: Long?,
    val templateId: Long,
    val templateVersion: Float?,
    val userIds: List<Long>?
)

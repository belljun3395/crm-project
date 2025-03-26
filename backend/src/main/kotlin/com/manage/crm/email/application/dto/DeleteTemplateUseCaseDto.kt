package com.manage.crm.email.application.dto

data class DeleteTemplateUseCaseIn(
    val emailTemplateId: Long,
    val forceFlag: Boolean
)

data class DeleteTemplateUseCaseOut(
    val success: Boolean
)

class DeleteTemplateUseCaseDto

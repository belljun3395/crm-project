package com.manage.crm.email.application.dto

data class PostTemplateUseCaseIn(
    val id: Long? = null,
    val templateName: String,
    val subject: String? = null,
    val version: Float? = null,
    val body: String,
    val variables: List<String>
)

data class PostTemplateUseCaseOut(
    val id: Long,
    val templateName: String,
    val version: Float?
)

class PostTemplateUseCaseDto

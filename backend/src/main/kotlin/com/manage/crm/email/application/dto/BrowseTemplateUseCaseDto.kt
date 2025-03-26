package com.manage.crm.email.application.dto

data class BrowseTemplateUseCaseIn(
    val withHistory: Boolean
)

data class BrowseTemplateUseCaseOut(
    val templates: List<TemplateWithHistoryDto>
)

data class TemplateWithHistoryDto(
    val template: TemplateDto,
    val histories: List<TemplateHistoryDto>
)

data class TemplateDto(
    val id: Long,
    val templateName: String,
    val subject: String,
    val body: String,
    val variables: List<String>,
    val version: Float,
    val createdAt: String
)

data class TemplateHistoryDto(
    val id: Long,
    val templateId: Long,
    val subject: String,
    val body: String,
    val variables: List<String>,
    val version: Float,
    val createdAt: String
)

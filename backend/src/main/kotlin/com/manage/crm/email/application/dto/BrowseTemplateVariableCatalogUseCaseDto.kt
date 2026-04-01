package com.manage.crm.email.application.dto

data class BrowseTemplateVariableCatalogUseCaseIn(
    val campaignId: Long?,
)

data class TemplateVariableCatalogItemDto(
    val key: String,
    val source: String,
    val description: String,
    val required: Boolean = false,
)

data class BrowseTemplateVariableCatalogUseCaseOut(
    val userVariables: List<TemplateVariableCatalogItemDto>,
    val campaignVariables: List<TemplateVariableCatalogItemDto>,
)

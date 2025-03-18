package com.manage.crm.email.controller.request

import io.swagger.v3.oas.annotations.media.Schema

data class PostTemplateRequest(
    val id: Long? = null,
    val templateName: String,
    val subject: String? = null,
    val version: Float? = null,
    val body: String,
    @Schema(description = "변수 리스트", example = "[\"name:default\", \"email\"]")
    val variables: List<String>? = emptyList()
)

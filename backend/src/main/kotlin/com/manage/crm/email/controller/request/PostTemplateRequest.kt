package com.manage.crm.email.controller.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PostTemplateRequest(
    @field:Min(value = 1, message = "Template ID must be positive")
    val id: Long? = null,
    @field:NotBlank(message = "Template name cannot be blank")
    @field:Size(max = 100, message = "Template name must be less than 100 characters")
    val templateName: String,
    @field:Size(max = 255, message = "Subject must be less than 255 characters")
    val subject: String? = null,
    @field:Min(value = 0, message = "Version cannot be negative")
    val version: Float? = null,
    @field:NotBlank(message = "Template body cannot be blank")
    val body: String,
    @Schema(description = "변수 리스트", example = "[\"name:default\", \"email\"]")
    val variables: List<String>? = emptyList()
)

package com.manage.crm.segment.controller.request

import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class SegmentConditionRequest(
    @field:NotBlank
    val field: String,
    @field:NotBlank
    val operator: String,
    @field:NotBlank
    val valueType: String,
    @field:NotNull
    val value: JsonNode
)

data class PostSegmentRequest(
    @field:NotBlank
    val name: String,
    val description: String? = null,
    val active: Boolean? = true,
    @field:NotEmpty
    val conditions: List<@Valid SegmentConditionRequest>
)

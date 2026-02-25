package com.manage.crm.segment.controller.request

import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

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
    @field:Pattern(regexp = SEGMENT_NAME_PATTERN, message = SEGMENT_NAME_PATTERN_MESSAGE)
    val name: String,
    val description: String? = null,
    val active: Boolean? = true,
    @field:NotEmpty
    val conditions: List<@Valid SegmentConditionRequest>
)

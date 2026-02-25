package com.manage.crm.segment.controller.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class PutSegmentRequest(
    @field:Pattern(regexp = SEGMENT_NAME_PATTERN, message = SEGMENT_NAME_PATTERN_MESSAGE)
    val name: String? = null,
    val description: String? = null,
    val active: Boolean? = null,
    @field:Size(min = 1, message = "conditions must not be empty when provided")
    val conditions: List<@Valid SegmentConditionRequest>? = null
)

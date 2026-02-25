package com.manage.crm.segment.controller.request

import jakarta.validation.Valid

data class PutSegmentRequest(
    val name: String? = null,
    val description: String? = null,
    val active: Boolean? = null,
    val conditions: List<@Valid SegmentConditionRequest>? = null
)

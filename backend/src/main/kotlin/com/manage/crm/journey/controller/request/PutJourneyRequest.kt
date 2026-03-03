package com.manage.crm.journey.controller.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class PutJourneyRequest(
    @field:NotBlank(message = "Journey name is required")
    val name: String,

    @field:NotBlank(message = "triggerType is required")
    val triggerType: String,

    val triggerEventName: String? = null,
    val triggerSegmentId: Long? = null,
    val triggerSegmentEvent: String? = null,
    val triggerSegmentWatchFields: List<String>? = null,
    val triggerSegmentCountThreshold: Long? = null,
    val active: Boolean? = null,

    @field:Valid
    @field:NotEmpty(message = "At least one step is required")
    val steps: List<PutJourneyStepRequest>
)

data class PutJourneyStepRequest(
    @field:Min(value = 1, message = "stepOrder must be greater than 0")
    val stepOrder: Int,

    @field:NotBlank(message = "stepType is required")
    val stepType: String,

    val channel: String? = null,
    val destination: String? = null,
    val subject: String? = null,
    val body: String? = null,
    val variables: Map<String, String>? = emptyMap(),
    val delayMillis: Long? = null,
    val conditionExpression: String? = null,

    @field:Min(value = 0, message = "retryCount must be zero or greater")
    val retryCount: Int? = 0
)

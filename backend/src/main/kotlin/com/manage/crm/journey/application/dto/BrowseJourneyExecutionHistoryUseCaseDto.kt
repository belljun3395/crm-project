package com.manage.crm.journey.application.dto

data class JourneyExecutionHistoryDto(
    val id: Long,
    val journeyExecutionId: Long,
    val journeyStepId: Long,
    val status: String,
    val attempt: Int,
    val message: String?,
    val idempotencyKey: String?,
    val createdAt: String?,
)

data class BrowseJourneyExecutionHistoryUseCaseIn(
    val journeyExecutionId: Long,
)

data class BrowseJourneyExecutionHistoryUseCaseOut(
    val histories: List<JourneyExecutionHistoryDto>,
)

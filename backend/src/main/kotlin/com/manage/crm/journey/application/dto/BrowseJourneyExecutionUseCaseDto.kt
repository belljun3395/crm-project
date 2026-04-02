package com.manage.crm.journey.application.dto

data class JourneyExecutionDto(
    val id: Long,
    val journeyId: Long,
    val eventId: Long,
    val userId: Long,
    val status: String,
    val currentStepOrder: Int,
    val lastError: String?,
    val triggerKey: String,
    val startedAt: String,
    val completedAt: String?,
    val createdAt: String,
    val updatedAt: String?,
)

data class BrowseJourneyExecutionUseCaseIn(
    val journeyId: Long?,
    val eventId: Long?,
    val userId: Long?,
)

data class BrowseJourneyExecutionUseCaseOut(
    val executions: List<JourneyExecutionDto>,
)

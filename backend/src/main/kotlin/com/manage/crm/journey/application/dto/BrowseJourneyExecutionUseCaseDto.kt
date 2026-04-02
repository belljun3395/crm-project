package com.manage.crm.journey.application.dto

data class BrowseJourneyExecutionUseCaseIn(
    val journeyId: Long?,
    val eventId: Long?,
    val userId: Long?,
)

data class BrowseJourneyExecutionUseCaseOut(
    val executions: List<JourneyExecutionDto>,
)

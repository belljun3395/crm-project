package com.manage.crm.journey.application.dto

data class BrowseJourneyExecutionHistoryUseCaseIn(
    val journeyExecutionId: Long,
)

data class BrowseJourneyExecutionHistoryUseCaseOut(
    val histories: List<JourneyExecutionHistoryDto>,
)

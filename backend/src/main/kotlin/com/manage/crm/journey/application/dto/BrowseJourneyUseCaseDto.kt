package com.manage.crm.journey.application.dto

data class BrowseJourneyUseCaseIn(
    val limit: Int = 50,
)

data class BrowseJourneyUseCaseOut(
    val journeys: List<JourneyDto>,
)

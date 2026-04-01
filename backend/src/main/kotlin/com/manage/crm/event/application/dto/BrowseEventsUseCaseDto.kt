package com.manage.crm.event.application.dto

data class BrowseEventsUseCaseIn(
    val limit: Int = 200,
)

data class BrowseEventsUseCaseOut(
    val events: List<EventDto>,
)

class BrowseEventsUseCaseDto

package com.manage.crm.journey.application.dto

import com.manage.crm.event.domain.Event

data class JourneyAutomationUseCaseIn(
    val event: Event? = null,
    val changedUserIds: List<Long>? = null,
)

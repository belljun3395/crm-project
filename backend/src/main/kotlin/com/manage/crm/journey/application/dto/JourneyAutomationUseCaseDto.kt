package com.manage.crm.journey.application.dto

data class JourneyAutomationUseCaseIn(
    val event: JourneyTriggerEvent? = null,
    val changedUserIds: List<Long>? = null,
)

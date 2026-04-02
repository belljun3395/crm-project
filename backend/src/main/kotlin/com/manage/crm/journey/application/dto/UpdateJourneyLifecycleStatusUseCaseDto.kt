package com.manage.crm.journey.application.dto

enum class JourneyLifecycleAction {
    PAUSE,
    RESUME,
    ARCHIVE,
}

data class UpdateJourneyLifecycleStatusUseCaseIn(
    val journeyId: Long,
    val action: JourneyLifecycleAction,
)

data class UpdateJourneyLifecycleStatusUseCaseOut(
    val journey: JourneyDto,
)

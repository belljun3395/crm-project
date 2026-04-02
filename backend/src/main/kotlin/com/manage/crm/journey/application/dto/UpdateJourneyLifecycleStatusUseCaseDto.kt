package com.manage.crm.journey.application.dto

data class UpdateJourneyLifecycleStatusUseCaseIn(
    val journeyId: Long,
    val action: JourneyLifecycleAction,
)

data class UpdateJourneyLifecycleStatusUseCaseOut(
    val journey: JourneyDto,
)

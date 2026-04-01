package com.manage.crm.journey.application.dto

import com.manage.crm.event.domain.Event

data class BrowseJourneyUseCaseIn(
    val dummy: Unit = Unit,
)

data class BrowseJourneyExecutionUseCaseIn(
    val journeyId: Long?,
    val eventId: Long?,
    val userId: Long?,
)

data class BrowseJourneyExecutionHistoryUseCaseIn(
    val journeyExecutionId: Long,
)

data class UpdateJourneyLifecycleStatusUseCaseIn(
    val journeyId: Long,
    val action: JourneyLifecycleAction,
)

data class JourneyAutomationUseCaseIn(
    val event: Event? = null,
    val changedUserIds: List<Long>? = null,
)

enum class JourneyLifecycleAction {
    PAUSE,
    RESUME,
    ARCHIVE,
}

@Deprecated("Use BrowseJourneyUseCaseIn")
typealias BrowseJourneyIn = BrowseJourneyUseCaseIn

@Deprecated("Use BrowseJourneyExecutionUseCaseIn")
typealias BrowseJourneyExecutionIn = BrowseJourneyExecutionUseCaseIn

@Deprecated("Use BrowseJourneyExecutionHistoryUseCaseIn")
typealias BrowseJourneyExecutionHistoryIn = BrowseJourneyExecutionHistoryUseCaseIn

@Deprecated("Use UpdateJourneyLifecycleStatusUseCaseIn")
typealias UpdateJourneyLifecycleIn = UpdateJourneyLifecycleStatusUseCaseIn

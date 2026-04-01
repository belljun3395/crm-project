package com.manage.crm.journey.application

// All DTOs and enums have been moved to com.manage.crm.journey.application.dto package
// Re-export for backward compatibility
@Deprecated(
    message = "Use com.manage.crm.journey.application.dto instead",
    replaceWith = ReplaceWith("JourneyTriggerType", "com.manage.crm.journey.application.dto.JourneyTriggerType"),
)
typealias JourneyTriggerType = com.manage.crm.journey.application.dto.JourneyTriggerType

@Deprecated(
    message = "Use com.manage.crm.journey.application.dto instead",
    replaceWith = ReplaceWith("JourneySegmentTriggerEventType", "com.manage.crm.journey.application.dto.JourneySegmentTriggerEventType"),
)
typealias JourneySegmentTriggerEventType = com.manage.crm.journey.application.dto.JourneySegmentTriggerEventType

@Deprecated(
    message = "Use com.manage.crm.journey.application.dto instead",
    replaceWith = ReplaceWith("JourneyStepType", "com.manage.crm.journey.application.dto.JourneyStepType"),
)
typealias JourneyStepType = com.manage.crm.journey.application.dto.JourneyStepType

@Deprecated(
    message = "Use com.manage.crm.journey.application.dto instead",
    replaceWith = ReplaceWith("JourneyExecutionStatus", "com.manage.crm.journey.application.dto.JourneyExecutionStatus"),
)
typealias JourneyExecutionStatus = com.manage.crm.journey.application.dto.JourneyExecutionStatus

@Deprecated(
    message = "Use com.manage.crm.journey.application.dto instead",
    replaceWith = ReplaceWith("JourneyExecutionHistoryStatus", "com.manage.crm.journey.application.dto.JourneyExecutionHistoryStatus"),
)
typealias JourneyExecutionHistoryStatus = com.manage.crm.journey.application.dto.JourneyExecutionHistoryStatus

@Deprecated(
    message = "Use com.manage.crm.journey.application.dto instead",
    replaceWith = ReplaceWith("JourneyLifecycleStatus", "com.manage.crm.journey.application.dto.JourneyLifecycleStatus"),
)
typealias JourneyLifecycleStatus = com.manage.crm.journey.application.dto.JourneyLifecycleStatus

@Deprecated(
    message = "Use com.manage.crm.journey.application.dto instead",
    replaceWith = ReplaceWith("PostJourneyStepIn", "com.manage.crm.journey.application.dto.PostJourneyStepIn"),
)
typealias PostJourneyStepIn = com.manage.crm.journey.application.dto.PostJourneyStepIn

@Deprecated(
    message = "Use com.manage.crm.journey.application.dto instead",
    replaceWith = ReplaceWith("PostJourneyIn", "com.manage.crm.journey.application.dto.PostJourneyIn"),
)
typealias PostJourneyIn = com.manage.crm.journey.application.dto.PostJourneyIn

@Deprecated(
    message = "Use com.manage.crm.journey.application.dto instead",
    replaceWith = ReplaceWith("JourneyStepDto", "com.manage.crm.journey.application.dto.JourneyStepDto"),
)
typealias JourneyStepDto = com.manage.crm.journey.application.dto.JourneyStepDto

@Deprecated(
    message = "Use com.manage.crm.journey.application.dto instead",
    replaceWith = ReplaceWith("JourneyDto", "com.manage.crm.journey.application.dto.JourneyDto"),
)
typealias JourneyDto = com.manage.crm.journey.application.dto.JourneyDto

@Deprecated(
    message = "Use com.manage.crm.journey.application.dto instead",
    replaceWith = ReplaceWith("JourneyExecutionDto", "com.manage.crm.journey.application.dto.JourneyExecutionDto"),
)
typealias JourneyExecutionDto = com.manage.crm.journey.application.dto.JourneyExecutionDto

@Deprecated(
    message = "Use com.manage.crm.journey.application.dto instead",
    replaceWith = ReplaceWith("JourneyExecutionHistoryDto", "com.manage.crm.journey.application.dto.JourneyExecutionHistoryDto"),
)
typealias JourneyExecutionHistoryDto = com.manage.crm.journey.application.dto.JourneyExecutionHistoryDto

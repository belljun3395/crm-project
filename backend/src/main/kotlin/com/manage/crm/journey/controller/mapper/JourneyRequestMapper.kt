package com.manage.crm.journey.controller.mapper

import com.manage.crm.journey.application.dto.JourneySegmentTriggerEventType
import com.manage.crm.journey.application.dto.JourneyStepType
import com.manage.crm.journey.application.dto.JourneyTriggerType
import com.manage.crm.journey.application.dto.PostJourneyStepIn
import com.manage.crm.journey.application.dto.PostJourneyUseCaseIn
import com.manage.crm.journey.application.dto.PutJourneyStepIn
import com.manage.crm.journey.application.dto.PutJourneyUseCaseIn
import com.manage.crm.journey.controller.request.PostJourneyRequest
import com.manage.crm.journey.controller.request.PostJourneyStepRequest
import com.manage.crm.journey.controller.request.PutJourneyRequest
import com.manage.crm.journey.controller.request.PutJourneyStepRequest

fun PostJourneyRequest.toUseCaseIn(): PostJourneyUseCaseIn =
    PostJourneyUseCaseIn(
        name = name,
        triggerType = JourneyTriggerType.from(triggerType),
        triggerEventName = triggerEventName,
        triggerSegmentId = triggerSegmentId,
        triggerSegmentEvent = triggerSegmentEvent?.let { JourneySegmentTriggerEventType.from(it) },
        triggerSegmentWatchFields = triggerSegmentWatchFields ?: emptyList(),
        triggerSegmentCountThreshold = triggerSegmentCountThreshold,
        active = active ?: true,
        steps = steps.map { it.toUseCaseIn() },
    )

fun PostJourneyStepRequest.toUseCaseIn(): PostJourneyStepIn =
    PostJourneyStepIn(
        stepOrder = stepOrder,
        stepType = JourneyStepType.from(stepType),
        channel = channel,
        destination = destination,
        subject = subject,
        body = body,
        variables = variables ?: emptyMap(),
        delayMillis = delayMillis,
        conditionExpression = conditionExpression,
        retryCount = retryCount ?: 0,
    )

fun PutJourneyRequest.toUseCaseIn(journeyId: Long): PutJourneyUseCaseIn =
    PutJourneyUseCaseIn(
        journeyId = journeyId,
        name = name,
        triggerType = JourneyTriggerType.from(triggerType),
        triggerEventName = triggerEventName,
        triggerSegmentId = triggerSegmentId,
        triggerSegmentEvent = triggerSegmentEvent?.let { JourneySegmentTriggerEventType.from(it) },
        triggerSegmentWatchFields = triggerSegmentWatchFields ?: emptyList(),
        triggerSegmentCountThreshold = triggerSegmentCountThreshold,
        active = active ?: throw IllegalArgumentException("active is required for updateJourney"),
        steps = steps.map { it.toUseCaseIn() },
    )

fun PutJourneyStepRequest.toUseCaseIn(): PutJourneyStepIn =
    PutJourneyStepIn(
        stepOrder = stepOrder,
        stepType = JourneyStepType.from(stepType),
        channel = channel,
        destination = destination,
        subject = subject,
        body = body,
        variables = variables ?: emptyMap(),
        delayMillis = delayMillis,
        conditionExpression = conditionExpression,
        retryCount = retryCount ?: 0,
    )

package com.manage.crm.journey.controller

import com.manage.crm.config.SwaggerTag
import com.manage.crm.journey.application.BrowseJourneyExecutionHistoryUseCase
import com.manage.crm.journey.application.BrowseJourneyExecutionIn
import com.manage.crm.journey.application.BrowseJourneyExecutionUseCase
import com.manage.crm.journey.application.BrowseJourneyUseCase
import com.manage.crm.journey.application.JourneyDto
import com.manage.crm.journey.application.JourneyExecutionDto
import com.manage.crm.journey.application.JourneyExecutionHistoryDto
import com.manage.crm.journey.application.JourneySegmentTriggerEventType
import com.manage.crm.journey.application.JourneyStepType
import com.manage.crm.journey.application.JourneyTriggerType
import com.manage.crm.journey.application.PostJourneyIn
import com.manage.crm.journey.application.PostJourneyStepIn
import com.manage.crm.journey.application.PostJourneyUseCase
import com.manage.crm.journey.application.PutJourneyIn
import com.manage.crm.journey.application.PutJourneyStepIn
import com.manage.crm.journey.application.PutJourneyUseCase
import com.manage.crm.journey.application.UpdateJourneyLifecycleStatusUseCase
import com.manage.crm.journey.controller.request.PostJourneyRequest
import com.manage.crm.journey.controller.request.PutJourneyRequest
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = SwaggerTag.JOURNEYS_SWAGGER_TAG, description = "여정 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/journeys"])
class JourneyController(
    private val postJourneyUseCase: PostJourneyUseCase,
    private val putJourneyUseCase: PutJourneyUseCase,
    private val updateJourneyLifecycleStatusUseCase: UpdateJourneyLifecycleStatusUseCase,
    private val browseJourneyUseCase: BrowseJourneyUseCase,
    private val browseJourneyExecutionUseCase: BrowseJourneyExecutionUseCase,
    private val browseJourneyExecutionHistoryUseCase: BrowseJourneyExecutionHistoryUseCase
) {
    @PostMapping
    suspend fun createJourney(
        @Valid
        @RequestBody
        request: PostJourneyRequest
    ): ApiResponse<ApiResponse.SuccessBody<JourneyDto>> {
        return postJourneyUseCase
            .execute(
                PostJourneyIn(
                    name = request.name,
                    triggerType = JourneyTriggerType.from(request.triggerType),
                    triggerEventName = request.triggerEventName,
                    triggerSegmentId = request.triggerSegmentId,
                    triggerSegmentEvent = request.triggerSegmentEvent?.let { JourneySegmentTriggerEventType.from(it) },
                    triggerSegmentWatchFields = request.triggerSegmentWatchFields ?: emptyList(),
                    triggerSegmentCountThreshold = request.triggerSegmentCountThreshold,
                    active = request.active ?: true,
                    steps = request.steps.map { step ->
                        PostJourneyStepIn(
                            stepOrder = step.stepOrder,
                            stepType = JourneyStepType.from(step.stepType),
                            channel = step.channel,
                            destination = step.destination,
                            subject = step.subject,
                            body = step.body,
                            variables = step.variables ?: emptyMap(),
                            delayMillis = step.delayMillis,
                            conditionExpression = step.conditionExpression,
                            retryCount = step.retryCount ?: 0
                        )
                    }
                )
            )
            .let { ApiResponseGenerator.success(it, HttpStatus.CREATED) }
    }

    @PutMapping("/{journeyId}")
    suspend fun updateJourney(
        @PathVariable journeyId: Long,
        @Valid
        @RequestBody
        request: PutJourneyRequest
    ): ApiResponse<ApiResponse.SuccessBody<JourneyDto>> {
        return putJourneyUseCase
            .execute(
                PutJourneyIn(
                    journeyId = journeyId,
                    name = request.name,
                    triggerType = JourneyTriggerType.from(request.triggerType),
                    triggerEventName = request.triggerEventName,
                    triggerSegmentId = request.triggerSegmentId,
                    triggerSegmentEvent = request.triggerSegmentEvent?.let { JourneySegmentTriggerEventType.from(it) },
                    triggerSegmentWatchFields = request.triggerSegmentWatchFields ?: emptyList(),
                    triggerSegmentCountThreshold = request.triggerSegmentCountThreshold,
                    active = request.active ?: throw IllegalArgumentException("active is required for updateJourney"),
                    steps = request.steps.map { step ->
                        PutJourneyStepIn(
                            stepOrder = step.stepOrder,
                            stepType = JourneyStepType.from(step.stepType),
                            channel = step.channel,
                            destination = step.destination,
                            subject = step.subject,
                            body = step.body,
                            variables = step.variables ?: emptyMap(),
                            delayMillis = step.delayMillis,
                            conditionExpression = step.conditionExpression,
                            retryCount = step.retryCount ?: 0
                        )
                    }
                )
            )
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @PostMapping("/{journeyId}/pause")
    suspend fun pauseJourney(
        @PathVariable journeyId: Long
    ): ApiResponse<ApiResponse.SuccessBody<JourneyDto>> {
        return updateJourneyLifecycleStatusUseCase.pause(journeyId)
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @PostMapping("/{journeyId}/resume")
    suspend fun resumeJourney(
        @PathVariable journeyId: Long
    ): ApiResponse<ApiResponse.SuccessBody<JourneyDto>> {
        return updateJourneyLifecycleStatusUseCase.resume(journeyId)
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @PostMapping("/{journeyId}/archive")
    suspend fun archiveJourney(
        @PathVariable journeyId: Long
    ): ApiResponse<ApiResponse.SuccessBody<JourneyDto>> {
        return updateJourneyLifecycleStatusUseCase.archive(journeyId)
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @GetMapping
    suspend fun browseJourneys(): ApiResponse<ApiResponse.SuccessBody<List<JourneyDto>>> {
        return browseJourneyUseCase.execute()
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @GetMapping("/executions")
    suspend fun browseExecutions(
        @RequestParam(required = false) journeyId: Long?,
        @RequestParam(required = false) eventId: Long?,
        @RequestParam(required = false) userId: Long?
    ): ApiResponse<ApiResponse.SuccessBody<List<JourneyExecutionDto>>> {
        return browseJourneyExecutionUseCase
            .execute(
                BrowseJourneyExecutionIn(
                    journeyId = journeyId,
                    eventId = eventId,
                    userId = userId
                )
            )
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @GetMapping("/executions/{executionId}/histories")
    suspend fun browseExecutionHistories(
        @PathVariable executionId: Long
    ): ApiResponse<ApiResponse.SuccessBody<List<JourneyExecutionHistoryDto>>> {
        return browseJourneyExecutionHistoryUseCase.execute(executionId)
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ApiResponse<ApiResponse.FailureBody> {
        return ApiResponseGenerator.fail(e.message ?: "invalid journey request", HttpStatus.BAD_REQUEST)
    }
}

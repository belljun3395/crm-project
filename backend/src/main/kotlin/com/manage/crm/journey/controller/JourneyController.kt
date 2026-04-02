package com.manage.crm.journey.controller

import com.manage.crm.config.SwaggerTag
import com.manage.crm.journey.application.BrowseJourneyExecutionHistoryUseCase
import com.manage.crm.journey.application.BrowseJourneyExecutionUseCase
import com.manage.crm.journey.application.BrowseJourneyUseCase
import com.manage.crm.journey.application.PostJourneyUseCase
import com.manage.crm.journey.application.PutJourneyUseCase
import com.manage.crm.journey.application.UpdateJourneyLifecycleStatusUseCase
import com.manage.crm.journey.application.dto.BrowseJourneyExecutionHistoryUseCaseIn
import com.manage.crm.journey.application.dto.BrowseJourneyExecutionUseCaseIn
import com.manage.crm.journey.application.dto.BrowseJourneyUseCaseIn
import com.manage.crm.journey.application.dto.JourneyDto
import com.manage.crm.journey.application.dto.JourneyExecutionDto
import com.manage.crm.journey.application.dto.JourneyExecutionHistoryDto
import com.manage.crm.journey.application.dto.JourneyLifecycleAction
import com.manage.crm.journey.application.dto.UpdateJourneyLifecycleStatusUseCaseIn
import com.manage.crm.journey.controller.mapper.toUseCaseIn
import com.manage.crm.journey.controller.request.PostJourneyRequest
import com.manage.crm.journey.controller.request.PutJourneyRequest
import com.manage.crm.journey.exception.InvalidJourneyException
import com.manage.crm.journey.exception.InvalidJourneyStepException
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
    private val browseJourneyExecutionHistoryUseCase: BrowseJourneyExecutionHistoryUseCase,
) {
    @PostMapping
    suspend fun createJourney(
        @Valid
        @RequestBody
        request: PostJourneyRequest,
    ): ApiResponse<ApiResponse.SuccessBody<JourneyDto>> =
        postJourneyUseCase
            .execute(request.toUseCaseIn())
            .let { ApiResponseGenerator.success(it.journey, HttpStatus.CREATED) }

    @PutMapping("/{journeyId}")
    suspend fun updateJourney(
        @PathVariable journeyId: Long,
        @Valid
        @RequestBody
        request: PutJourneyRequest,
    ): ApiResponse<ApiResponse.SuccessBody<JourneyDto>> =
        putJourneyUseCase
            .execute(request.toUseCaseIn(journeyId))
            .let { ApiResponseGenerator.success(it.journey, HttpStatus.OK) }

    @PostMapping("/{journeyId}/pause")
    suspend fun pauseJourney(
        @PathVariable journeyId: Long,
    ): ApiResponse<ApiResponse.SuccessBody<JourneyDto>> =
        updateJourneyLifecycleStatusUseCase
            .execute(
                UpdateJourneyLifecycleStatusUseCaseIn(
                    journeyId = journeyId,
                    action = JourneyLifecycleAction.PAUSE,
                ),
            ).let { ApiResponseGenerator.success(it.journey, HttpStatus.OK) }

    @PostMapping("/{journeyId}/resume")
    suspend fun resumeJourney(
        @PathVariable journeyId: Long,
    ): ApiResponse<ApiResponse.SuccessBody<JourneyDto>> =
        updateJourneyLifecycleStatusUseCase
            .execute(
                UpdateJourneyLifecycleStatusUseCaseIn(
                    journeyId = journeyId,
                    action = JourneyLifecycleAction.RESUME,
                ),
            ).let { ApiResponseGenerator.success(it.journey, HttpStatus.OK) }

    @PostMapping("/{journeyId}/archive")
    suspend fun archiveJourney(
        @PathVariable journeyId: Long,
    ): ApiResponse<ApiResponse.SuccessBody<JourneyDto>> =
        updateJourneyLifecycleStatusUseCase
            .execute(
                UpdateJourneyLifecycleStatusUseCaseIn(
                    journeyId = journeyId,
                    action = JourneyLifecycleAction.ARCHIVE,
                ),
            ).let { ApiResponseGenerator.success(it.journey, HttpStatus.OK) }

    @GetMapping
    suspend fun browseJourneys(
        @RequestParam(defaultValue = "50") limit: Int,
    ): ApiResponse<ApiResponse.SuccessBody<List<JourneyDto>>> =
        browseJourneyUseCase
            .execute(BrowseJourneyUseCaseIn(limit = limit))
            .let { ApiResponseGenerator.success(it.journeys, HttpStatus.OK) }

    @GetMapping("/executions")
    suspend fun browseExecutions(
        @RequestParam(required = false) journeyId: Long?,
        @RequestParam(required = false) eventId: Long?,
        @RequestParam(required = false) userId: Long?,
    ): ApiResponse<ApiResponse.SuccessBody<List<JourneyExecutionDto>>> =
        browseJourneyExecutionUseCase
            .execute(
                BrowseJourneyExecutionUseCaseIn(
                    journeyId = journeyId,
                    eventId = eventId,
                    userId = userId,
                ),
            ).let { ApiResponseGenerator.success(it.executions, HttpStatus.OK) }

    @GetMapping("/executions/{executionId}/histories")
    suspend fun browseExecutionHistories(
        @PathVariable executionId: Long,
    ): ApiResponse<ApiResponse.SuccessBody<List<JourneyExecutionHistoryDto>>> =
        browseJourneyExecutionHistoryUseCase
            .execute(BrowseJourneyExecutionHistoryUseCaseIn(executionId))
            .let { ApiResponseGenerator.success(it.histories, HttpStatus.OK) }

    @ExceptionHandler(InvalidJourneyException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidJourneyException(e: InvalidJourneyException): ApiResponse<ApiResponse.FailureBody> =
        ApiResponseGenerator.fail(e.message ?: "invalid journey request", HttpStatus.BAD_REQUEST)

    @ExceptionHandler(InvalidJourneyStepException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidJourneyStepException(e: InvalidJourneyStepException): ApiResponse<ApiResponse.FailureBody> =
        ApiResponseGenerator.fail(e.message ?: "invalid journey step", HttpStatus.BAD_REQUEST)

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ApiResponse<ApiResponse.FailureBody> =
        ApiResponseGenerator.fail(e.message ?: "invalid journey request", HttpStatus.BAD_REQUEST)
}

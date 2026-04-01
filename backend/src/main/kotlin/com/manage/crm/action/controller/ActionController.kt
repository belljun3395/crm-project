package com.manage.crm.action.controller

import com.manage.crm.action.application.ActionChannel
import com.manage.crm.action.application.ActionDispatchHistoryDto
import com.manage.crm.action.application.ActionDispatchIn
import com.manage.crm.action.application.ActionDispatchOut
import com.manage.crm.action.application.ActionDispatchService
import com.manage.crm.action.controller.request.PostActionDispatchRequest
import com.manage.crm.config.SwaggerTag
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = SwaggerTag.ACTIONS_SWAGGER_TAG, description = "액션 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/actions"])
class ActionController(
    private val actionDispatchService: ActionDispatchService,
) {
    @PostMapping("/dispatch")
    suspend fun dispatch(
        @Valid
        @RequestBody
        request: PostActionDispatchRequest,
    ): ApiResponse<ApiResponse.SuccessBody<ActionDispatchOut>> =
        actionDispatchService
            .dispatch(
                ActionDispatchIn(
                    channel = ActionChannel.from(request.channel),
                    destination = request.destination,
                    subject = request.subject,
                    body = request.body,
                    variables = request.variables ?: emptyMap(),
                    campaignId = request.campaignId,
                    journeyExecutionId = request.journeyExecutionId,
                ),
            ).let { ApiResponseGenerator.success(it, HttpStatus.OK) }

    @GetMapping("/dispatch/histories")
    suspend fun browseDispatchHistories(
        @RequestParam(required = false) campaignId: Long?,
        @RequestParam(required = false) journeyExecutionId: Long?,
    ): ApiResponse<ApiResponse.SuccessBody<List<ActionDispatchHistoryDto>>> =
        actionDispatchService
            .browse(campaignId = campaignId, journeyExecutionId = journeyExecutionId)
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ApiResponse<ApiResponse.FailureBody> =
        ApiResponseGenerator.fail(e.message ?: "invalid action request", HttpStatus.BAD_REQUEST)
}

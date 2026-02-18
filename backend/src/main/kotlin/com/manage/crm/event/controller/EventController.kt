package com.manage.crm.event.controller

import com.manage.crm.config.SwaggerTag
import com.manage.crm.event.application.PostCampaignUseCase
import com.manage.crm.event.application.PostEventUseCase
import com.manage.crm.event.application.SearchEventsUseCase
import com.manage.crm.event.application.dto.PostCampaignPropertyDto
import com.manage.crm.event.application.dto.PostCampaignUseCaseIn
import com.manage.crm.event.application.dto.PostCampaignUseCaseOut
import com.manage.crm.event.application.dto.PostEventPropertyDto
import com.manage.crm.event.application.dto.PostEventUseCaseIn
import com.manage.crm.event.application.dto.PostEventUseCaseOut
import com.manage.crm.event.application.dto.PropertyAndOperationDto
import com.manage.crm.event.application.dto.SearchEventPropertyDto
import com.manage.crm.event.application.dto.SearchEventsUseCaseIn
import com.manage.crm.event.application.dto.SearchEventsUseCaseOut
import com.manage.crm.event.controller.request.PostCampaignRequest
import com.manage.crm.event.controller.request.PostEventRequest
import com.manage.crm.event.domain.JoinOperation
import com.manage.crm.event.domain.Operation
import com.manage.crm.event.exception.InvalidSearchConditionException
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = SwaggerTag.EVENT_SWAGGER_TAG, description = "이벤트 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/events"])
class EventController(
    private val postEventUseCase: PostEventUseCase,
    private val searchEventsUseCase: SearchEventsUseCase,
    private val postCampaignUseCase: PostCampaignUseCase
) {

    @PostMapping
    suspend fun postEvent(
        @RequestBody request: PostEventRequest
    ): ApiResponse<ApiResponse.SuccessBody<PostEventUseCaseOut>> {
        return postEventUseCase
            .execute(
                PostEventUseCaseIn(
                    name = request.name,
                    campaignName = request.campaignName,
                    externalId = request.externalId,
                    properties = request.properties.map {
                        PostEventPropertyDto(
                            key = it.key,
                            value = it.value
                        )
                    }
                )
            )
            .let { ApiResponseGenerator.success(it, HttpStatus.CREATED) }
    }

    @Parameters(
        Parameter(
            name = "where",
            description = """
검색 조건을 설정합니다.
형식: key&value(&key&value)&operation&joinOperation,...
- key(필수): 검색할 이벤트의 속성 키
- value(필수): 검색할 이벤트의 속성 값
- operation(필수): =, !=, >, >=, <, <=, like, between
    - between: 동일한 key로 두 개의 value를 입력
- joinOperation(필수): and, or, end(마지막)
ex) key1&value1&operation&joinOperation,key2&value2&operation&joinOperation...
            """
        )
    )
    @GetMapping
    suspend fun searchEvents(
        @RequestParam eventName: String,
        @RequestParam where: String
    ): ApiResponse<ApiResponse.SuccessBody<SearchEventsUseCaseOut>> {
        return searchEventsUseCase
            .execute(
                SearchEventsUseCaseIn(
                    eventName = eventName,
                    propertyAndOperations = parseWhereClause(where)
                )
            )
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @ExceptionHandler(InvalidSearchConditionException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidSearchConditionException(e: InvalidSearchConditionException): ApiResponse<ApiResponse.FailureBody> {
        return ApiResponseGenerator.fail(e.message ?: "invalid search condition", HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/campaign")
    suspend fun postCampaign(@RequestBody request: PostCampaignRequest): ApiResponse<ApiResponse.SuccessBody<PostCampaignUseCaseOut>> {
        return postCampaignUseCase
            .execute(
                PostCampaignUseCaseIn(
                    name = request.name,
                    properties = request.properties.map {
                        PostCampaignPropertyDto(
                            key = it.key,
                            value = it.value
                        )
                    }
                )
            )
            .let { ApiResponseGenerator.success(it, HttpStatus.CREATED) }
    }

    private fun parseWhereClause(where: String): List<PropertyAndOperationDto> {
        if (where.isBlank()) {
            throw InvalidSearchConditionException("where parameter is required")
        }

        return where.split(",").mapIndexed { index, expression ->
            val tokens = expression.split("&")
            if (tokens.size < 4 || tokens.size % 2 != 0) {
                throw InvalidSearchConditionException("Invalid where format at index $index")
            }

            val operation = runCatching { Operation.fromValue(tokens[tokens.lastIndex - 1]) }
                .getOrElse {
                    throw InvalidSearchConditionException("Invalid operation at index $index: ${tokens[tokens.lastIndex - 1]}")
                }

            val joinOperation = runCatching { JoinOperation.fromValue(tokens.last()) }
                .getOrElse {
                    throw InvalidSearchConditionException("Invalid join operation at index $index: ${tokens.last()}")
                }

            val properties = tokens
                .dropLast(2)
                .chunked(2)
                .map { (key, value) ->
                    if (key.isBlank() || value.isBlank()) {
                        throw InvalidSearchConditionException("Empty key/value is not allowed at index $index")
                    }
                    SearchEventPropertyDto(key = key, value = value)
                }

            if (properties.size != operation.paramsCnt) {
                throw InvalidSearchConditionException(
                    "Operation ${operation.name} requires ${operation.paramsCnt} value(s) at index $index"
                )
            }

            if (operation == Operation.BETWEEN && properties.map { it.key }.distinct().size != 1) {
                throw InvalidSearchConditionException("Between operation requires the same key at index $index")
            }

            PropertyAndOperationDto(
                properties = properties,
                operation = operation,
                joinOperation = joinOperation
            )
        }
    }
}

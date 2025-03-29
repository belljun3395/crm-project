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
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
                    propertyAndOperations = where.split(",").map {
                        // TODO extract to function in support class
                        val count = it.split("&").count()
                        val propertySource = mutableListOf<SearchEventPropertyDto>()
                        for (i in 0 until count - 2 step 2) {
                            propertySource.add(
                                SearchEventPropertyDto(
                                    key = it.split("&")[i],
                                    value = it.split("&")[i + 1]
                                )
                            )
                        }
                        PropertyAndOperationDto(
                            properties = propertySource,
                            operation = Operation.fromValue(it.split("&")[count - 2]),
                            joinOperation = JoinOperation.fromValue(it.split("&")[count - 1])
                        )
                    }.toList()
                )
            )
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
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
}

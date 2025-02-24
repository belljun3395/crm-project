package com.manage.crm.event.controller

import com.manage.crm.config.SwaggerTag
import com.manage.crm.event.application.PostEventUseCase
import com.manage.crm.event.application.dto.PostEventPropertyDto
import com.manage.crm.event.application.dto.PostEventUseCaseIn
import com.manage.crm.event.application.dto.PostEventUseCaseOut
import com.manage.crm.event.controller.request.PostEventRequest
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = SwaggerTag.EVENT_SWAGGER_TAG, description = "이벤트 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/events"])
class EventController(
    private val postEventUseCase: PostEventUseCase
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
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }
}

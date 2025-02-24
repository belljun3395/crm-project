package com.manage.crm.event.controller

import com.manage.crm.config.SwaggerTag
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = SwaggerTag.EVENT_SWAGGER_TAG, description = "이벤트 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/events"])
class EventController

package com.manage.crm.user.controller

import com.manage.crm.config.SwaggerTag
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = SwaggerTag.USERS_SWAGGER_TAG, description = "사용자 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/users"])
class UserController()

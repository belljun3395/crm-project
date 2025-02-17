package com.manage.crm.user.controller

import com.manage.crm.config.SwaggerTag
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import com.manage.crm.user.application.BrowseUserUseCase
import com.manage.crm.user.application.EnrollUserUseCase
import com.manage.crm.user.application.dto.BrowseUsersUseCaseOut
import com.manage.crm.user.application.dto.EnrollUserUseCaseIn
import com.manage.crm.user.application.dto.EnrollUserUseCaseOut
import com.manage.crm.user.controller.request.EnrollUserRequest
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = SwaggerTag.USERS_SWAGGER_TAG, description = "사용자 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/users"])
class UserController(
    private val browseUsersUseCase: BrowseUserUseCase,
    private val enrollUserUseCase: EnrollUserUseCase
) {

    @GetMapping
    suspend fun browseUsers(): ApiResponse<ApiResponse.SuccessBody<BrowseUsersUseCaseOut>> {
        return browseUsersUseCase
            .execute()
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @PostMapping
    suspend fun enrollUser(
        @RequestBody request: EnrollUserRequest
    ): ApiResponse<ApiResponse.SuccessBody<EnrollUserUseCaseOut>> {
        return enrollUserUseCase
            .execute(
                EnrollUserUseCaseIn(
                    id = request.id,
                    externalId = request.externalId,
                    userAttributes = request.userAttributes
                )
            )
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }
}

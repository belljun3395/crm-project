package com.manage.crm.email.controller

import com.manage.crm.config.SwaggerTag
import com.manage.crm.email.application.BrowseTemplateUseCase
import com.manage.crm.email.application.dto.BrowseTemplateUseCaseIn
import com.manage.crm.email.application.dto.BrowseTemplateUseCaseOut
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = SwaggerTag.EMAILS_SWAGGER_TAG, description = "이메일 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/emails"])
class EmailController(
    private val browseTemplateUseCase: BrowseTemplateUseCase
) {
    @GetMapping(value = ["/templates"])
    suspend fun browseEmailTemplates(
        @RequestParam(required = false) history: Boolean?
    ): ApiResponse<ApiResponse.SuccessBody<BrowseTemplateUseCaseOut>> {
        return browseTemplateUseCase
            .execute(BrowseTemplateUseCaseIn(withHistory = history ?: true))
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }
}

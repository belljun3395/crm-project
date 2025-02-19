package com.manage.crm.email.controller

import com.manage.crm.config.SwaggerTag
import com.manage.crm.email.application.BrowseEmailNotificationSchedulesUseCase
import com.manage.crm.email.application.BrowseTemplateUseCase
import com.manage.crm.email.application.PostTemplateUseCase
import com.manage.crm.email.application.SendNotificationEmailUseCase
import com.manage.crm.email.application.dto.BrowseEmailNotificationSchedulesUseCaseOut
import com.manage.crm.email.application.dto.BrowseTemplateUseCaseIn
import com.manage.crm.email.application.dto.BrowseTemplateUseCaseOut
import com.manage.crm.email.application.dto.PostTemplateUseCaseIn
import com.manage.crm.email.application.dto.PostTemplateUseCaseOut
import com.manage.crm.email.application.dto.SendNotificationEmailUseCaseIn
import com.manage.crm.email.application.dto.SendNotificationEmailUseCaseOut
import com.manage.crm.email.controller.request.PostTemplateRequest
import com.manage.crm.email.controller.request.SendNotificationEmailRequest
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = SwaggerTag.EMAILS_SWAGGER_TAG, description = "이메일 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/emails"])
class EmailController(
    private val browseTemplateUseCase: BrowseTemplateUseCase,
    private val postTemplateUseCase: PostTemplateUseCase,
    private val sendNotificationEmailUseCase: SendNotificationEmailUseCase,
    private val browseEmailNotificationSchedulesUseCase: BrowseEmailNotificationSchedulesUseCase
) {
    @GetMapping(value = ["/templates"])
    suspend fun browseEmailTemplates(
        @RequestParam(required = false) history: Boolean?
    ): ApiResponse<ApiResponse.SuccessBody<BrowseTemplateUseCaseOut>> {
        return browseTemplateUseCase
            .execute(BrowseTemplateUseCaseIn(withHistory = history ?: true))
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @PostMapping(value = ["/templates"])
    suspend fun postEmailTemplate(
        @RequestBody request: PostTemplateRequest
    ): ApiResponse<ApiResponse.SuccessBody<PostTemplateUseCaseOut>> {
        return postTemplateUseCase
            .execute(
                PostTemplateUseCaseIn(
                    id = request.id,
                    templateName = request.templateName,
                    subject = request.subject,
                    version = request.version,
                    body = request.body,
                    variables = request.variables ?: emptyList()
                )
            )
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @PostMapping(value = ["/send/notifications/email"])
    suspend fun sendNotificationEmail(
        @RequestBody request: SendNotificationEmailRequest
    ): ApiResponse<ApiResponse.SuccessBody<SendNotificationEmailUseCaseOut>> {
        return sendNotificationEmailUseCase
            .execute(
                SendNotificationEmailUseCaseIn(
                    templateId = request.templateId,
                    templateVersion = request.templateVersion,
                    userIds = request.userIds ?: emptyList()
                )
            )
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @GetMapping(value = ["/schedules/notifications/email"])
    suspend fun browseEmailNotificationSchedules(): ApiResponse<ApiResponse.SuccessBody<BrowseEmailNotificationSchedulesUseCaseOut>> {
        return browseEmailNotificationSchedulesUseCase
            .execute()
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }
}

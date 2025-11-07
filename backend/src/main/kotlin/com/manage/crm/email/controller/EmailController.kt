package com.manage.crm.email.controller

import com.manage.crm.config.SwaggerTag
import com.manage.crm.email.application.BrowseEmailNotificationSchedulesUseCase
import com.manage.crm.email.application.BrowseEmailSendHistoriesUseCase
import com.manage.crm.email.application.BrowseTemplateUseCase
import com.manage.crm.email.application.CancelNotificationEmailUseCase
import com.manage.crm.email.application.DeleteTemplateUseCase
import com.manage.crm.email.application.PostEmailNotificationSchedulesUseCase
import com.manage.crm.email.application.PostTemplateUseCase
import com.manage.crm.email.application.SendNotificationEmailUseCase
import com.manage.crm.email.application.dto.BrowseEmailNotificationSchedulesUseCaseOut
import com.manage.crm.email.application.dto.BrowseEmailSendHistoriesUseCaseIn
import com.manage.crm.email.application.dto.BrowseEmailSendHistoriesUseCaseOut
import com.manage.crm.email.application.dto.BrowseTemplateUseCaseIn
import com.manage.crm.email.application.dto.BrowseTemplateUseCaseOut
import com.manage.crm.email.application.dto.CancelNotificationEmailUseCaseIn
import com.manage.crm.email.application.dto.CancelNotificationEmailUseCaseOut
import com.manage.crm.email.application.dto.DeleteTemplateUseCaseIn
import com.manage.crm.email.application.dto.DeleteTemplateUseCaseOut
import com.manage.crm.email.application.dto.PostEmailNotificationSchedulesUseCaseIn
import com.manage.crm.email.application.dto.PostEmailNotificationSchedulesUseCaseOut
import com.manage.crm.email.application.dto.PostTemplateUseCaseIn
import com.manage.crm.email.application.dto.PostTemplateUseCaseOut
import com.manage.crm.email.application.dto.SendNotificationEmailUseCaseIn
import com.manage.crm.email.application.dto.SendNotificationEmailUseCaseOut
import com.manage.crm.email.controller.request.PostNotificationEmailRequest
import com.manage.crm.email.controller.request.PostTemplateRequest
import com.manage.crm.email.controller.request.SendNotificationEmailRequest
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
    private val deleteTemplateUseCase: DeleteTemplateUseCase,
    private val sendNotificationEmailUseCase: SendNotificationEmailUseCase,
    private val browseEmailNotificationSchedulesUseCase: BrowseEmailNotificationSchedulesUseCase,
    private val postEmailNotificationSchedulesUseCase: PostEmailNotificationSchedulesUseCase,
    private val cancelNotificationEmailUseCase: CancelNotificationEmailUseCase,
    private val browseEmailSendHistoriesUseCase: BrowseEmailSendHistoriesUseCase
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

    @PostMapping(value = ["/send/notifications"])
    suspend fun sendNotificationEmail(
        @RequestBody request: SendNotificationEmailRequest
    ): ApiResponse<ApiResponse.SuccessBody<SendNotificationEmailUseCaseOut>> {
        return sendNotificationEmailUseCase
            .execute(
                SendNotificationEmailUseCaseIn(
                    templateId = request.templateId,
                    templateVersion = request.templateVersion,
                    userIds = request.userIds ?: emptyList(),
                    campaignId = request.campaignId
                )
            )
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @DeleteMapping(value = ["/templates/{templateId}"])
    suspend fun deleteEmailTemplate(
        @PathVariable("templateId") templateId: Long,
        @RequestParam(required = false) force: Boolean?
    ): ApiResponse<ApiResponse.SuccessBody<DeleteTemplateUseCaseOut>> {
        return deleteTemplateUseCase
            .execute(
                DeleteTemplateUseCaseIn(
                    emailTemplateId = templateId,
                    forceFlag = force ?: false
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

    @PostMapping(value = ["/schedules/notifications/email"])
    suspend fun postEmailNotificationSchedule(
        @RequestBody request: PostNotificationEmailRequest
    ): ApiResponse<ApiResponse.SuccessBody<PostEmailNotificationSchedulesUseCaseOut>> {
        return postEmailNotificationSchedulesUseCase
            .execute(
                PostEmailNotificationSchedulesUseCaseIn(
                    templateId = request.templateId,
                    templateVersion = request.templateVersion,
                    userIds = request.userIds,
                    expiredTime = request.expiredTime
                )
            )
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @DeleteMapping(value = ["/schedules/notifications/email/{scheduleId}"])
    suspend fun cancelEmailNotificationSchedule(
        @PathVariable("scheduleId") scheduleId: String
    ): ApiResponse<ApiResponse.SuccessBody<CancelNotificationEmailUseCaseOut>> {
        return cancelNotificationEmailUseCase
            .execute(CancelNotificationEmailUseCaseIn(EventId(scheduleId)))
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @GetMapping(value = ["/histories"])
    suspend fun browseEmailSendHistories(
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) sendStatus: String?
    ): ApiResponse<ApiResponse.SuccessBody<BrowseEmailSendHistoriesUseCaseOut>> {
        return browseEmailSendHistoriesUseCase
            .execute(BrowseEmailSendHistoriesUseCaseIn(userId = userId, sendStatus = sendStatus))
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }
}

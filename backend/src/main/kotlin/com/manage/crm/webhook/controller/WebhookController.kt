package com.manage.crm.webhook.controller

import com.manage.crm.config.SwaggerTag
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import com.manage.crm.webhook.application.BrowseWebhookUseCase
import com.manage.crm.webhook.application.DeleteWebhookUseCase
import com.manage.crm.webhook.application.GetWebhookUseCase
import com.manage.crm.webhook.application.PostWebhookUseCase
import com.manage.crm.webhook.application.dto.BrowseWebhookUseCaseIn
import com.manage.crm.webhook.application.dto.DeleteWebhookUseCaseIn
import com.manage.crm.webhook.application.dto.GetWebhookUseCaseIn
import com.manage.crm.webhook.application.dto.PostWebhookUseCaseIn
import com.manage.crm.webhook.application.dto.PostWebhookUseCaseOut
import com.manage.crm.webhook.application.dto.WebhookDto
import com.manage.crm.webhook.controller.request.PostWebhookRequest
import com.manage.crm.webhook.controller.request.PutWebhookRequest
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = SwaggerTag.WEBHOOKS_SWAGGER_TAG, description = "웹훅 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/webhooks"])
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true")
class WebhookController(
    private val postWebhookUseCase: PostWebhookUseCase,
    private val deleteWebhookUseCase: DeleteWebhookUseCase,
    private val browseWebhookUseCase: BrowseWebhookUseCase,
    private val getWebhookUseCase: GetWebhookUseCase
) {
    @PostMapping
    suspend fun create(
        @Valid
        @RequestBody
        request: PostWebhookRequest
    ): ApiResponse<ApiResponse.SuccessBody<PostWebhookUseCaseOut>> {
        return postWebhookUseCase.execute(
            PostWebhookUseCaseIn(
                name = request.name,
                url = request.url,
                events = request.events,
                active = request.active
            )
        ).let { ApiResponseGenerator.success(it, HttpStatus.CREATED) }
    }

    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: Long,
        @Valid
        @RequestBody
        request: PutWebhookRequest
    ): ApiResponse<ApiResponse.SuccessBody<PostWebhookUseCaseOut>> {
        val existing = getWebhookUseCase.execute(GetWebhookUseCaseIn(id)).webhook
        return postWebhookUseCase.execute(
            PostWebhookUseCaseIn(
                id = id,
                name = request.name ?: existing.name,
                url = request.url ?: existing.url,
                events = request.events ?: existing.events,
                active = request.active ?: existing.active
            )
        ).let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: Long): ApiResponse<ApiResponse.Success> {
        deleteWebhookUseCase.execute(DeleteWebhookUseCaseIn(id))
        return ApiResponseGenerator.success(HttpStatus.NO_CONTENT)
    }

    @GetMapping
    suspend fun list(): ApiResponse<ApiResponse.SuccessBody<List<WebhookDto>>> {
        return browseWebhookUseCase.execute(BrowseWebhookUseCaseIn())
            .let { ApiResponseGenerator.success(it.webhooks, HttpStatus.OK) }
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: Long): ApiResponse<ApiResponse.SuccessBody<WebhookDto>> {
        return getWebhookUseCase.execute(GetWebhookUseCaseIn(id))
            .let { ApiResponseGenerator.success(it.webhook, HttpStatus.OK) }
    }
}

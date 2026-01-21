package com.manage.crm.webhook.controller

import com.manage.crm.config.SwaggerTag
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import com.manage.crm.webhook.application.ManageWebhookUseCase
import com.manage.crm.webhook.application.WebhookQueryService
import com.manage.crm.webhook.domain.CreateWebhookRequest
import com.manage.crm.webhook.domain.UpdateWebhookRequest
import com.manage.crm.webhook.domain.WebhookResponse
import io.swagger.v3.oas.annotations.tags.Tag
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
class WebhookController(
    private val manageWebhookUseCase: ManageWebhookUseCase,
    private val webhookQueryService: WebhookQueryService
) {
    @PostMapping
    suspend fun create(@RequestBody request: CreateWebhookRequest): ApiResponse<ApiResponse.SuccessBody<WebhookResponse>> {
        return manageWebhookUseCase.create(request)
            .let { ApiResponseGenerator.success(it, HttpStatus.CREATED) }
    }

    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdateWebhookRequest
    ): ApiResponse<ApiResponse.SuccessBody<WebhookResponse>> {
        return manageWebhookUseCase.update(id, request)
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: Long): ApiResponse<ApiResponse.Success> {
        manageWebhookUseCase.delete(id)
        return ApiResponseGenerator.success(HttpStatus.NO_CONTENT)
    }

    @GetMapping
    suspend fun list(): ApiResponse<ApiResponse.SuccessBody<List<WebhookResponse>>> {
        return webhookQueryService.list()
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: Long): ApiResponse<ApiResponse.SuccessBody<WebhookResponse>> {
        return webhookQueryService.get(id)
            .let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }
}

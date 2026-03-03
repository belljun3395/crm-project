package com.manage.crm.webhook.controller

import com.manage.crm.audit.application.AuditLogService
import com.manage.crm.audit.application.dto.RecordAuditLogCommand
import com.manage.crm.config.SwaggerTag
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import com.manage.crm.webhook.application.BrowseWebhookDeadLettersUseCase
import com.manage.crm.webhook.application.BrowseWebhookDeliveryLogsUseCase
import com.manage.crm.webhook.application.BrowseWebhookUseCase
import com.manage.crm.webhook.application.DeleteWebhookUseCase
import com.manage.crm.webhook.application.GetWebhookUseCase
import com.manage.crm.webhook.application.PostWebhookUseCase
import com.manage.crm.webhook.application.RetryWebhookDeadLettersUseCase
import com.manage.crm.webhook.application.dto.BrowseWebhookDeadLettersUseCaseIn
import com.manage.crm.webhook.application.dto.BrowseWebhookDeliveryLogsUseCaseIn
import com.manage.crm.webhook.application.dto.BrowseWebhookUseCaseIn
import com.manage.crm.webhook.application.dto.DeleteWebhookUseCaseIn
import com.manage.crm.webhook.application.dto.GetWebhookUseCaseIn
import com.manage.crm.webhook.application.dto.PostWebhookUseCaseIn
import com.manage.crm.webhook.application.dto.PostWebhookUseCaseOut
import com.manage.crm.webhook.application.dto.RetryWebhookDeadLettersUseCaseIn
import com.manage.crm.webhook.application.dto.RetryWebhookDeadLettersUseCaseOut
import com.manage.crm.webhook.application.dto.WebhookDeadLetterDto
import com.manage.crm.webhook.application.dto.WebhookDeadLetterRetryResultDto
import com.manage.crm.webhook.application.dto.WebhookDeliveryLogDto
import com.manage.crm.webhook.application.dto.WebhookDto
import com.manage.crm.webhook.controller.request.PostWebhookDeadLetterRetryRequest
import com.manage.crm.webhook.controller.request.PostWebhookRequest
import com.manage.crm.webhook.controller.request.PutWebhookRequest
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = SwaggerTag.WEBHOOKS_SWAGGER_TAG, description = "웹훅 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/webhooks"])
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
class WebhookController(
    private val postWebhookUseCase: PostWebhookUseCase,
    private val deleteWebhookUseCase: DeleteWebhookUseCase,
    private val browseWebhookUseCase: BrowseWebhookUseCase,
    private val getWebhookUseCase: GetWebhookUseCase,
    private val browseWebhookDeliveryLogsUseCase: BrowseWebhookDeliveryLogsUseCase,
    private val browseWebhookDeadLettersUseCase: BrowseWebhookDeadLettersUseCase,
    private val retryWebhookDeadLettersUseCase: RetryWebhookDeadLettersUseCase,
    private val auditLogService: AuditLogService
) {
    @PostMapping
    suspend fun create(
        @Valid
        @RequestBody
        request: PostWebhookRequest,
        httpRequest: ServerHttpRequest
    ): ApiResponse<ApiResponse.SuccessBody<PostWebhookUseCaseOut>> {
        val result = postWebhookUseCase.execute(
            PostWebhookUseCaseIn(
                name = request.name,
                url = request.url,
                events = request.events,
                active = request.active
            )
        )
        auditLogService.record(
            RecordAuditLogCommand(
                actorId = extractActorId(httpRequest),
                action = "WEBHOOK_CREATE",
                resourceType = "WEBHOOK",
                resourceId = result.id.toString(),
                requestMethod = httpRequest.method.name(),
                requestPath = httpRequest.path.value(),
                statusCode = HttpStatus.CREATED.value(),
                detail = "created webhook name=${result.name}"
            )
        )
        return ApiResponseGenerator.success(result, HttpStatus.CREATED)
    }

    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: Long,
        @Valid
        @RequestBody
        request: PutWebhookRequest,
        httpRequest: ServerHttpRequest
    ): ApiResponse<ApiResponse.SuccessBody<PostWebhookUseCaseOut>> {
        val existing = getWebhookUseCase.execute(GetWebhookUseCaseIn(id)).webhook
        val result = postWebhookUseCase.execute(
            PostWebhookUseCaseIn(
                id = id,
                name = request.name ?: existing.name,
                url = request.url ?: existing.url,
                events = request.events ?: existing.events,
                active = request.active ?: existing.active
            )
        )
        auditLogService.record(
            RecordAuditLogCommand(
                actorId = extractActorId(httpRequest),
                action = "WEBHOOK_UPDATE",
                resourceType = "WEBHOOK",
                resourceId = result.id.toString(),
                requestMethod = httpRequest.method.name(),
                requestPath = httpRequest.path.value(),
                statusCode = HttpStatus.OK.value(),
                detail = "updated webhook name=${result.name}"
            )
        )
        return ApiResponseGenerator.success(result, HttpStatus.OK)
    }

    @DeleteMapping("/{id}")
    suspend fun delete(
        @PathVariable id: Long,
        httpRequest: ServerHttpRequest
    ): ApiResponse<ApiResponse.Success> {
        deleteWebhookUseCase.execute(DeleteWebhookUseCaseIn(id))
        auditLogService.record(
            RecordAuditLogCommand(
                actorId = extractActorId(httpRequest),
                action = "WEBHOOK_DELETE",
                resourceType = "WEBHOOK",
                resourceId = id.toString(),
                requestMethod = httpRequest.method.name(),
                requestPath = httpRequest.path.value(),
                statusCode = HttpStatus.NO_CONTENT.value(),
                detail = "deleted webhook"
            )
        )
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

    @GetMapping("/{id}/deliveries")
    suspend fun listDeliveries(
        @PathVariable id: Long,
        @RequestParam(required = false, defaultValue = "50") limit: Int
    ): ApiResponse<ApiResponse.SuccessBody<List<WebhookDeliveryLogDto>>> {
        return browseWebhookDeliveryLogsUseCase.execute(
            BrowseWebhookDeliveryLogsUseCaseIn(
                webhookId = id,
                limit = limit
            )
        ).let { ApiResponseGenerator.success(it.deliveries, HttpStatus.OK) }
    }

    @GetMapping("/{id}/dead-letters")
    suspend fun listDeadLetters(
        @PathVariable id: Long,
        @RequestParam(required = false, defaultValue = "50") limit: Int
    ): ApiResponse<ApiResponse.SuccessBody<List<WebhookDeadLetterDto>>> {
        return browseWebhookDeadLettersUseCase.execute(
            BrowseWebhookDeadLettersUseCaseIn(
                webhookId = id,
                limit = limit
            )
        ).let { ApiResponseGenerator.success(it.deadLetters, HttpStatus.OK) }
    }

    @PostMapping("/{id}/dead-letters/{deadLetterId}/retry")
    suspend fun retryDeadLetter(
        @PathVariable id: Long,
        @PathVariable deadLetterId: Long,
        httpRequest: ServerHttpRequest
    ): ApiResponse<ApiResponse.SuccessBody<WebhookDeadLetterRetryResultDto>> {
        val result = retryWebhookDeadLettersUseCase.retrySingle(id, deadLetterId)
        auditLogService.record(
            RecordAuditLogCommand(
                actorId = extractActorId(httpRequest),
                action = "WEBHOOK_DEAD_LETTER_RETRY",
                resourceType = "WEBHOOK",
                resourceId = id.toString(),
                requestMethod = httpRequest.method.name(),
                requestPath = httpRequest.path.value(),
                statusCode = HttpStatus.OK.value(),
                detail = "retried deadLetterId=$deadLetterId status=${result.status}"
            )
        )
        return ApiResponseGenerator.success(result, HttpStatus.OK)
    }

    @PostMapping("/{id}/dead-letters/retry")
    suspend fun retryDeadLetters(
        @PathVariable id: Long,
        @RequestBody(required = false) request: PostWebhookDeadLetterRetryRequest?,
        httpRequest: ServerHttpRequest
    ): ApiResponse<ApiResponse.SuccessBody<RetryWebhookDeadLettersUseCaseOut>> {
        val result = retryWebhookDeadLettersUseCase.retryBatch(
            RetryWebhookDeadLettersUseCaseIn(
                webhookId = id,
                deadLetterIds = request?.deadLetterIds ?: emptyList(),
                limit = request?.limit ?: 50
            )
        )
        auditLogService.record(
            RecordAuditLogCommand(
                actorId = extractActorId(httpRequest),
                action = "WEBHOOK_DEAD_LETTER_RETRY_BATCH",
                resourceType = "WEBHOOK",
                resourceId = id.toString(),
                requestMethod = httpRequest.method.name(),
                requestPath = httpRequest.path.value(),
                statusCode = HttpStatus.OK.value(),
                detail = "retried deadLetters count=${result.results.size}"
            )
        )
        return ApiResponseGenerator.success(result, HttpStatus.OK)
    }

    private fun extractActorId(httpRequest: ServerHttpRequest): String? {
        return httpRequest.headers.getFirst("X-Actor-Id")
    }
}

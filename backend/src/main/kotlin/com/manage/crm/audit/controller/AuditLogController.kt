package com.manage.crm.audit.controller

import com.manage.crm.audit.application.BrowseAuditLogsUseCase
import com.manage.crm.audit.application.dto.AuditLogDto
import com.manage.crm.audit.application.dto.BrowseAuditLogsUseCaseIn
import com.manage.crm.config.SwaggerTag
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = SwaggerTag.AUDIT_SWAGGER_TAG, description = "감사 로그 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/audit-logs"])
class AuditLogController(
    private val browseAuditLogsUseCase: BrowseAuditLogsUseCase
) {
    @GetMapping
    suspend fun list(
        @RequestParam(required = false, defaultValue = "50") limit: Int,
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) resourceType: String?,
        @RequestParam(required = false) actorId: String?
    ): ApiResponse<ApiResponse.SuccessBody<List<AuditLogDto>>> {
        return browseAuditLogsUseCase.execute(
            BrowseAuditLogsUseCaseIn(
                limit = limit,
                action = action,
                resourceType = resourceType,
                actorId = actorId
            )
        ).let { ApiResponseGenerator.success(it.logs, HttpStatus.OK) }
    }
}

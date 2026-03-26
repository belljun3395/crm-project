package com.manage.crm.segment.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.config.SwaggerTag
import com.manage.crm.segment.application.BrowseSegmentUseCase
import com.manage.crm.segment.application.DeleteSegmentUseCase
import com.manage.crm.segment.application.GetSegmentUseCase
import com.manage.crm.segment.application.PostSegmentUseCase
import com.manage.crm.segment.application.dto.BrowseSegmentUseCaseIn
import com.manage.crm.segment.application.dto.GetSegmentUseCaseIn
import com.manage.crm.segment.application.dto.PostSegmentConditionIn
import com.manage.crm.segment.application.dto.PostSegmentUseCaseIn
import com.manage.crm.segment.application.dto.PostSegmentUseCaseOut
import com.manage.crm.segment.application.dto.SegmentDto
import com.manage.crm.segment.controller.request.PostSegmentRequest
import com.manage.crm.segment.controller.request.PutSegmentRequest
import com.manage.crm.segment.exception.InvalidSegmentConditionException
import com.manage.crm.segment.service.SegmentTargetingService
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import com.manage.crm.user.domain.repository.UserRepository
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.format.DateTimeFormatter

@Tag(name = SwaggerTag.SEGMENTS_SWAGGER_TAG, description = "세그먼트 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/segments"])
class SegmentController(
    private val postSegmentUseCase: PostSegmentUseCase,
    private val deleteSegmentUseCase: DeleteSegmentUseCase,
    private val browseSegmentUseCase: BrowseSegmentUseCase,
    private val getSegmentUseCase: GetSegmentUseCase,
    private val segmentTargetingService: SegmentTargetingService,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    @PostMapping
    suspend fun create(
        @Valid
        @RequestBody
        request: PostSegmentRequest
    ): ApiResponse<ApiResponse.SuccessBody<PostSegmentUseCaseOut>> {
        return postSegmentUseCase.execute(
            PostSegmentUseCaseIn(
                name = request.name,
                description = request.description,
                active = request.active ?: true,
                conditions = request.conditions.map { condition ->
                    PostSegmentConditionIn(
                        field = condition.field,
                        operator = condition.operator,
                        valueType = condition.valueType,
                        value = condition.value
                    )
                }
            )
        ).let { ApiResponseGenerator.success(it, HttpStatus.CREATED) }
    }

    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: Long,
        @Valid
        @RequestBody
        request: PutSegmentRequest
    ): ApiResponse<ApiResponse.SuccessBody<PostSegmentUseCaseOut>> {
        val existing = getSegmentUseCase.execute(GetSegmentUseCaseIn(id)).segment
        val conditions = request.conditions?.map { condition ->
            PostSegmentConditionIn(
                field = condition.field,
                operator = condition.operator,
                valueType = condition.valueType,
                value = condition.value
            )
        } ?: existing.conditions.map { condition ->
            PostSegmentConditionIn(
                field = condition.field,
                operator = condition.operator,
                valueType = condition.valueType,
                value = condition.value
            )
        }

        return postSegmentUseCase.execute(
            PostSegmentUseCaseIn(
                id = id,
                name = request.name ?: existing.name,
                description = request.description ?: existing.description,
                active = request.active ?: existing.active,
                conditions = conditions
            )
        ).let { ApiResponseGenerator.success(it, HttpStatus.OK) }
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: Long): ApiResponse<Void> {
        deleteSegmentUseCase.execute(id)
        return ApiResponseGenerator.fail(HttpStatus.NO_CONTENT)
    }

    @GetMapping
    suspend fun list(
        @RequestParam(required = false, defaultValue = "50") limit: Int
    ): ApiResponse<ApiResponse.SuccessBody<List<SegmentDto>>> {
        return browseSegmentUseCase.execute(
            BrowseSegmentUseCaseIn(limit = limit)
        ).let { ApiResponseGenerator.success(it.segments, HttpStatus.OK) }
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: Long): ApiResponse<ApiResponse.SuccessBody<SegmentDto>> {
        return getSegmentUseCase.execute(GetSegmentUseCaseIn(id))
            .let { ApiResponseGenerator.success(it.segment, HttpStatus.OK) }
    }

    @GetMapping("/{id}/users")
    suspend fun getMatchedUsers(
        @PathVariable id: Long,
        @RequestParam(required = false) campaignId: Long?
    ): ApiResponse<ApiResponse.SuccessBody<List<SegmentMatchedUserDto>>> {
        val targetUserIds = segmentTargetingService.resolveUserIds(id, campaignId)
        if (targetUserIds.isEmpty()) {
            return ApiResponseGenerator.success(emptyList(), HttpStatus.OK)
        }

        val targetIdSet = targetUserIds.toSet()
        val users = mutableListOf<com.manage.crm.user.domain.User>()
        var page = 0
        val size = 500
        while (true) {
            val batch = userRepository.findAllWithPagination(page, size)
            if (batch.isEmpty()) {
                break
            }
            users += batch.filter { user -> user.id != null && targetIdSet.contains(user.id) }
            if (batch.size < size) {
                break
            }
            page += 1
        }

        val matchedUsers = users.mapNotNull { user ->
            val userId = user.id ?: return@mapNotNull null
            val userAttributes = runCatching { objectMapper.readTree(user.userAttributes.value) }.getOrNull()
            SegmentMatchedUserDto(
                id = userId,
                externalId = user.externalId,
                email = userAttributes?.get("email")?.asText(),
                name = userAttributes?.get("name")?.asText(),
                createdAt = user.createdAt?.format(formatter)
            )
        }.sortedBy { it.id }

        return ApiResponseGenerator.success(matchedUsers, HttpStatus.OK)
    }

    @ExceptionHandler(InvalidSegmentConditionException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidSegmentConditionException(e: InvalidSegmentConditionException): ApiResponse<ApiResponse.FailureBody> {
        return ApiResponseGenerator.fail(e.message ?: "invalid segment condition", HttpStatus.BAD_REQUEST)
    }
}

data class SegmentMatchedUserDto(
    val id: Long,
    val externalId: String,
    val email: String?,
    val name: String?,
    val createdAt: String?
)

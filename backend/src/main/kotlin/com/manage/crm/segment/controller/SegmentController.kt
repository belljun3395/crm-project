package com.manage.crm.segment.controller

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
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
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

@Tag(name = SwaggerTag.SEGMENTS_SWAGGER_TAG, description = "세그먼트 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/segments"])
class SegmentController(
    private val postSegmentUseCase: PostSegmentUseCase,
    private val deleteSegmentUseCase: DeleteSegmentUseCase,
    private val browseSegmentUseCase: BrowseSegmentUseCase,
    private val getSegmentUseCase: GetSegmentUseCase
) {
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

    @ExceptionHandler(InvalidSegmentConditionException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidSegmentConditionException(e: InvalidSegmentConditionException): ApiResponse<ApiResponse.FailureBody> {
        return ApiResponseGenerator.fail(e.message ?: "invalid segment condition", HttpStatus.BAD_REQUEST)
    }
}

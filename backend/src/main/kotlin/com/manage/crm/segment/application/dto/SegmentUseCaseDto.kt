package com.manage.crm.segment.application.dto

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.manage.crm.segment.domain.Segment
import com.manage.crm.segment.domain.SegmentCondition
import java.time.format.DateTimeFormatter

private val SEGMENT_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

data class PostSegmentUseCaseIn(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
    val conditions: List<PostSegmentConditionIn>,
)

data class PostSegmentConditionIn(
    val field: String,
    val operator: String,
    val valueType: String,
    val value: JsonNode,
)

data class PostSegmentUseCaseOut(
    val segment: SegmentDto,
)

data class BrowseSegmentUseCaseIn(
    val limit: Int = 50,
)

data class BrowseSegmentUseCaseOut(
    val segments: List<SegmentDto>,
)

data class GetSegmentUseCaseIn(
    val id: Long,
)

data class GetSegmentUseCaseOut(
    val segment: SegmentDto,
)

data class DeleteSegmentUseCaseIn(
    val id: Long,
)

data class GetSegmentMatchedUsersUseCaseIn(
    val segmentId: Long,
    val campaignId: Long? = null,
)

data class GetSegmentMatchedUsersUseCaseOut(
    val users: List<SegmentMatchedUserDto>,
)

data class SegmentDto(
    val id: Long,
    val name: String,
    val description: String?,
    val active: Boolean,
    val conditions: List<SegmentConditionDto>,
    val createdAt: String?,
)

data class SegmentConditionDto(
    val field: String,
    val operator: String,
    val valueType: String,
    val value: JsonNode,
    val position: Int,
)

data class SegmentMatchedUserDto(
    val id: Long,
    val externalId: String,
    val email: String?,
    val name: String?,
    val createdAt: String?,
)

fun Segment.toSegmentDto(conditions: List<SegmentConditionDto>): SegmentDto {
    val segmentId = this.id ?: throw IllegalStateException("Segment id is null")
    return SegmentDto(
        id = segmentId,
        name = this.name,
        description = this.description,
        active = this.active,
        conditions = conditions,
        createdAt = this.createdAt?.format(SEGMENT_DATE_TIME_FORMATTER),
    )
}

fun SegmentCondition.toSegmentConditionDto(objectMapper: ObjectMapper): SegmentConditionDto =
    SegmentConditionDto(
        field = this.fieldName,
        operator = this.operator,
        valueType = this.valueType,
        value = runCatching { objectMapper.readTree(this.conditionValue) }.getOrElse { NullNode.instance },
        position = this.position,
    )

fun PostSegmentConditionIn.toSegmentConditionDto(position: Int): SegmentConditionDto =
    SegmentConditionDto(
        field = this.field,
        operator = this.operator.uppercase(),
        valueType = this.valueType.uppercase(),
        value = this.value,
        position = position,
    )

fun SegmentConditionDto.toPostSegmentConditionIn(): PostSegmentConditionIn =
    PostSegmentConditionIn(
        field = this.field,
        operator = this.operator,
        valueType = this.valueType,
        value = this.value,
    )

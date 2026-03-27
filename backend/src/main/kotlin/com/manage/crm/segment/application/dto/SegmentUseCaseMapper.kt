package com.manage.crm.segment.application.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.manage.crm.segment.domain.Segment
import com.manage.crm.segment.domain.SegmentCondition
import java.time.format.DateTimeFormatter

private val SEGMENT_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun Segment.toSegmentDto(
    conditions: List<SegmentConditionDto>
): SegmentDto {
    val segmentId = this.id ?: throw IllegalStateException("Segment id is null")
    return SegmentDto(
        id = segmentId,
        name = this.name,
        description = this.description,
        active = this.active,
        conditions = conditions,
        createdAt = this.createdAt?.format(SEGMENT_DATE_TIME_FORMATTER)
    )
}

fun SegmentCondition.toSegmentConditionDto(objectMapper: ObjectMapper): SegmentConditionDto {
    return SegmentConditionDto(
        field = this.fieldName,
        operator = this.operator,
        valueType = this.valueType,
        value = runCatching { objectMapper.readTree(this.conditionValue) }.getOrElse { NullNode.instance },
        position = this.position
    )
}

fun PostSegmentConditionIn.toSegmentConditionDto(position: Int): SegmentConditionDto {
    return SegmentConditionDto(
        field = this.field,
        operator = this.operator.uppercase(),
        valueType = this.valueType.uppercase(),
        value = this.value,
        position = position
    )
}

fun SegmentConditionDto.toPostSegmentConditionIn(): PostSegmentConditionIn {
    return PostSegmentConditionIn(
        field = this.field,
        operator = this.operator,
        valueType = this.valueType,
        value = this.value
    )
}

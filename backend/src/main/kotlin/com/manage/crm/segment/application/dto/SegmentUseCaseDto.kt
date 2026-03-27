package com.manage.crm.segment.application.dto

import com.fasterxml.jackson.databind.JsonNode

data class PostSegmentUseCaseIn(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
    val conditions: List<PostSegmentConditionIn>
)

data class PostSegmentConditionIn(
    val field: String,
    val operator: String,
    val valueType: String,
    val value: JsonNode
)

data class PostSegmentUseCaseOut(
    val segment: SegmentDto
)

data class BrowseSegmentUseCaseIn(
    val limit: Int = 50
)

data class BrowseSegmentUseCaseOut(
    val segments: List<SegmentDto>
)

data class GetSegmentUseCaseIn(
    val id: Long
)

data class GetSegmentUseCaseOut(
    val segment: SegmentDto
)

data class DeleteSegmentUseCaseIn(
    val id: Long
)

data class GetSegmentMatchedUsersUseCaseIn(
    val segmentId: Long,
    val campaignId: Long? = null
)

data class GetSegmentMatchedUsersUseCaseOut(
    val users: List<SegmentMatchedUserDto>
)

data class SegmentDto(
    val id: Long,
    val name: String,
    val description: String?,
    val active: Boolean,
    val conditions: List<SegmentConditionDto>,
    val createdAt: String?
)

data class SegmentConditionDto(
    val field: String,
    val operator: String,
    val valueType: String,
    val value: JsonNode,
    val position: Int
)

data class SegmentMatchedUserDto(
    val id: Long,
    val externalId: String,
    val email: String?,
    val name: String?,
    val createdAt: String?
)

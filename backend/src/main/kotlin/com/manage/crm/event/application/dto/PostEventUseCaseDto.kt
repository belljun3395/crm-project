package com.manage.crm.event.application.dto

data class PostEventUseCaseIn(
    val name: String,
    val externalId: String,
    val properties: List<PostEventPropertyDto>
)

data class PostEventPropertyDto(
    val key: String,
    val value: String
)

data class PostEventUseCaseOut(
    val id: Long
)
class PostEventUseCaseDto

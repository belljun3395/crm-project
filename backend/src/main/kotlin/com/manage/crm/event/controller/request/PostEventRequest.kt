package com.manage.crm.event.controller.request

data class PostEventRequest(
    val name: String,
    val externalId: String,
    val properties: List<PostEventPropertyDto>
)

data class PostEventPropertyDto(
    val key: String,
    val value: String
)

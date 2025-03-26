package com.manage.crm.user.controller.request

data class EnrollUserRequest(
    val id: Long?,
    val externalId: String,
    val userAttributes: String
)

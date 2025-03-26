package com.manage.crm.user.application.dto

data class EnrollUserUseCaseIn(
    val id: Long?,
    val externalId: String,
    val userAttributes: String
)

data class EnrollUserUseCaseOut(
    val id: Long,
    val externalId: String,
    val userAttributes: String
)

class EnrollUserUseCaseDto

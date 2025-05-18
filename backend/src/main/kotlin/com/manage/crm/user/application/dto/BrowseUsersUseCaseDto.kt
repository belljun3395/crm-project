package com.manage.crm.user.application.dto

import java.time.LocalDateTime

class BrowseUsersUseCaseIn

data class BrowseUsersUseCaseOut(
    val users: List<UserDto>
)
data class UserDto(
    val id: Long,
    val externalId: String,
    val userAttributes: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
class BrowseUsersUseCaseDto

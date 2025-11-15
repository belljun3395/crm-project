package com.manage.crm.user.application.dto

import com.manage.crm.support.web.PageResponse
import java.time.LocalDateTime

data class BrowseUsersUseCaseIn(
    val page: Int = 0,
    val size: Int = 20
)

data class BrowseUsersUseCaseOut(
    val users: PageResponse<UserDto>
)

data class UserDto(
    val id: Long,
    val externalId: String,
    val userAttributes: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

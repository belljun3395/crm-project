package com.manage.crm.user.application.dto

class BrowseUsersUseCaseIn

data class BrowseUsersUseCaseOut(
    val users: List<UserDto>
)
data class UserDto(
    val id: Long,
    val externalId: String,
    val userAttributes: String
)
class BrowseUsersUseCaseDto

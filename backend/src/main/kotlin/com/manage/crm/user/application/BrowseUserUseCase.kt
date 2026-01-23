package com.manage.crm.user.application

import com.manage.crm.support.out
import com.manage.crm.support.web.PageResponse
import com.manage.crm.user.application.dto.BrowseUsersUseCaseIn
import com.manage.crm.user.application.dto.BrowseUsersUseCaseOut
import com.manage.crm.user.application.dto.UserDto
import com.manage.crm.user.domain.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class BrowseUserUseCase(
    private val userRepository: UserRepository
) {
    suspend fun execute(input: BrowseUsersUseCaseIn): BrowseUsersUseCaseOut {
        val searchQuery = input.query?.takeIf { it.isNotBlank() }
        val users = if (searchQuery == null) {
            userRepository.findAllWithPagination(input.page, input.size)
        } else {
            userRepository.searchUsers(searchQuery, input.page, input.size)
        }
        val totalElements = if (searchQuery == null) {
            userRepository.countAll()
        } else {
            userRepository.countSearchUsers(searchQuery)
        }

        val userDtos = users.map { user ->
            UserDto(
                id = user.id!!,
                externalId = user.externalId,
                userAttributes = user.userAttributes.value,
                updatedAt = user.updatedAt!!,
                createdAt = user.createdAt!!
            )
        }

        return out {
            BrowseUsersUseCaseOut(
                users = PageResponse.of(
                    content = userDtos,
                    page = input.page,
                    size = input.size,
                    totalElements = totalElements
                )
            )
        }
    }
}

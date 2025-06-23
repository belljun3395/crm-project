package com.manage.crm.user.application

import com.manage.crm.support.out
import com.manage.crm.user.application.dto.BrowseUsersUseCaseOut
import com.manage.crm.user.application.dto.UserDto
import com.manage.crm.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class BrowseUserUseCase(
    private val userRepository: UserRepository
) {
    suspend fun execute(): BrowseUsersUseCaseOut {
        val users = userRepository.findAll().toList()

        return out {
            BrowseUsersUseCaseOut(
                users = users.map { user ->
                    UserDto(
                        id = user.id!!,
                        externalId = user.externalId,
                        userAttributes = user.userAttributes.value,
                        updatedAt = user.updatedAt!!,
                        createdAt = user.createdAt!!
                    )
                }
            )
        }
    }
}

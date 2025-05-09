package com.manage.crm.user.application

import com.manage.crm.user.application.dto.BrowseUsersUseCaseOut
import com.manage.crm.user.application.dto.UserDto
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.Json
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.asFlow
import java.time.LocalDateTime
import kotlin.random.Random

class BrowseUserUseCaseTest : BehaviorSpec({
    lateinit var userRepository: UserRepository
    lateinit var useCase: BrowseUserUseCase

    beforeContainer {
        userRepository = mockk()
        useCase = BrowseUserUseCase(userRepository)
    }

    fun userStubs(size: Int): List<User> =
        (1..size).map {
            User.new(
                id = it.toLong(),
                externalId = it.toString(),
                userAttributes = Json(
                    """
                    {
                        "email": "example$it@example.com",
                        "name": "example$it"
                    }
                    """.trimIndent()
                ),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }

    given("BrowseUserUseCase") {
        `when`("browse all users") {
            val userSize = Random(1).nextInt(1, 10)
            val expectedUsers = userStubs(userSize)
            coEvery { userRepository.findAll() } answers { expectedUsers.asFlow() }

            val result = useCase.execute()
            then("should return BrowseUsersUseCaseOut") {
                result shouldBe BrowseUsersUseCaseOut(
                    users = expectedUsers.map {
                        UserDto(
                            id = it.id!!,
                            externalId = it.externalId!!,
                            userAttributes = it.userAttributes?.value!!
                        )
                    }
                )
            }
            then("findAll users") {
                coVerify(exactly = 1) { userRepository.findAll() }
            }
        }
    }
})

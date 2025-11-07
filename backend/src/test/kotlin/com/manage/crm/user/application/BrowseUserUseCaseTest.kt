package com.manage.crm.user.application

import com.manage.crm.support.web.PageResponse
import com.manage.crm.user.application.dto.BrowseUsersUseCaseIn
import com.manage.crm.user.application.dto.BrowseUsersUseCaseOut
import com.manage.crm.user.application.dto.UserDto
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.UserAttributes
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class BrowseUserUseCaseTest : BehaviorSpec({
    lateinit var userRepository: UserRepository
    lateinit var useCase: BrowseUserUseCase

    beforeContainer {
        userRepository = mockk()
        useCase = BrowseUserUseCase(userRepository)
    }

    fun userStubs(size: Int, startId: Int = 1): List<User> =
        (startId until startId + size).map {
            User.new(
                id = it.toLong(),
                externalId = it.toString(),
                userAttributes = UserAttributes(
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
        `when`("browse users with pagination - first page") {
            val page = 0
            val size = 5
            val totalElements = 25L
            val expectedUsers = userStubs(size, 1)

            coEvery { userRepository.findAllWithPagination(page, size) } returns expectedUsers
            coEvery { userRepository.countAll() } returns totalElements

            val input = BrowseUsersUseCaseIn(page = page, size = size)
            val result = useCase.execute(input)

            then("should return paginated users with correct page info") {
                result shouldBe BrowseUsersUseCaseOut(
                    users = PageResponse.of(
                        content = expectedUsers.map {
                            UserDto(
                                id = it.id!!,
                                externalId = it.externalId,
                                userAttributes = it.userAttributes.value,
                                createdAt = it.createdAt!!,
                                updatedAt = it.updatedAt!!
                            )
                        },
                        page = page,
                        size = size,
                        totalElements = totalElements
                    )
                )
                result.users.page shouldBe 0
                result.users.size shouldBe 5
                result.users.totalElements shouldBe 25
                result.users.totalPages shouldBe 5
                result.users.content.size shouldBe 5
            }

            then("should call repository methods correctly") {
                coVerify(exactly = 1) { userRepository.findAllWithPagination(page, size) }
                coVerify(exactly = 1) { userRepository.countAll() }
            }
        }

        `when`("browse users with pagination - second page") {
            val page = 1
            val size = 10
            val totalElements = 25L
            val expectedUsers = userStubs(size, 11)

            coEvery { userRepository.findAllWithPagination(page, size) } returns expectedUsers
            coEvery { userRepository.countAll() } returns totalElements

            val input = BrowseUsersUseCaseIn(page = page, size = size)
            val result = useCase.execute(input)

            then("should return correct second page") {
                result.users.page shouldBe 1
                result.users.size shouldBe 10
                result.users.totalElements shouldBe 25
                result.users.totalPages shouldBe 3
                result.users.content.size shouldBe 10
            }
        }

        `when`("browse users with default pagination parameters") {
            val page = 0
            val size = 20
            val totalElements = 15L
            val expectedUsers = userStubs(15, 1)

            coEvery { userRepository.findAllWithPagination(page, size) } returns expectedUsers
            coEvery { userRepository.countAll() } returns totalElements

            val input = BrowseUsersUseCaseIn()
            val result = useCase.execute(input)

            then("should use default page and size values") {
                result.users.page shouldBe 0
                result.users.size shouldBe 20
                result.users.totalElements shouldBe 15
                result.users.totalPages shouldBe 1
                result.users.content.size shouldBe 15
            }
        }
    }
})

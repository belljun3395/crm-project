package com.manage.crm.user.application

import com.manage.crm.user.application.dto.EnrollUserUseCaseIn
import com.manage.crm.user.application.dto.EnrollUserUseCaseOut
import com.manage.crm.user.application.service.JsonService
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.Json
import com.manage.crm.user.domain.vo.RequiredUserAttributeKey
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class EnrollUserUseCaseTest : BehaviorSpec({
    lateinit var userRepository: UserRepository
    lateinit var jsonService: JsonService
    lateinit var useCase: EnrollUserUseCase

    beforeContainer {
        userRepository = mockk()
        jsonService = mockk()
        useCase = EnrollUserUseCase(userRepository, jsonService)
    }

    given("EnrollUserUseCase") {
        `when`("save new user") {
            val useCaseIn = EnrollUserUseCaseIn(
                id = null,
                externalId = "1",
                userAttributes = """
                    {
                        "email": "example@example.com",
                        "name": "example"
                    }
                """.trimIndent()
            )

            val userAttributes = Json(useCaseIn.userAttributes)
            coEvery {
                jsonService.execute(useCaseIn.userAttributes, RequiredUserAttributeKey.EMAIL)
            } answers {
                userAttributes
            }
            val expectedUser = User.new(
                id = 1,
                externalId = useCaseIn.externalId,
                userAttributes = userAttributes,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            coEvery { userRepository.save(any()) } answers {
                expectedUser
            }

            val result = useCase.execute(useCaseIn)
            then("should return EnrollUserUseCaseOut") {
                result shouldBe EnrollUserUseCaseOut(
                    id = 1,
                    externalId = expectedUser.externalId!!,
                    userAttributes = expectedUser.userAttributes!!.value
                )
            }
            then("userAttributes should contain Email key") {
                coVerify(exactly = 1) {
                    jsonService.execute(useCaseIn.userAttributes, RequiredUserAttributeKey.EMAIL)
                }
            }

            then("save user") {
                coVerify(exactly = 1) {
                    userRepository.save(
                        match {
                            it.externalId == expectedUser.externalId &&
                                it.userAttributes == expectedUser.userAttributes
                        }
                    )
                }
            }
        }

        `when`("modify user") {
            val useCaseIn = EnrollUserUseCaseIn(
                id = 1,
                externalId = "1",
                userAttributes = """
                    {
                        "email": "example@example.com",
                        "name": "example",
                        "age": 20
                    }
                """.trimIndent()
            )

            val userUpdateAttributes = Json(useCaseIn.userAttributes)
            coEvery {
                jsonService.execute(useCaseIn.userAttributes, RequiredUserAttributeKey.EMAIL)
            } answers {
                userUpdateAttributes
            }

            val originUser = User.new(
                id = 1,
                externalId = useCaseIn.externalId,
                userAttributes = userUpdateAttributes,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            coEvery { userRepository.findById(any()) } answers {
                originUser
            }

            originUser.updateAttributes(userUpdateAttributes)
            val expectedUser = originUser

            coEvery { userRepository.save(any()) } returns expectedUser

            val result = useCase.execute(useCaseIn)
            then("should return EnrollUserUseCaseOut") {
                result shouldBe EnrollUserUseCaseOut(
                    id = 1,
                    externalId = expectedUser.externalId!!,
                    userAttributes = expectedUser.userAttributes!!.value
                )
            }
            then("userAttributes should contain Email key") {
                coVerify(exactly = 1) {
                    jsonService.execute(
                        useCaseIn.userAttributes,
                        RequiredUserAttributeKey.EMAIL
                    )
                }
            }
            then("find user by id for update") {
                coVerify(exactly = 1) {
                    userRepository.findById(useCaseIn.id!!)
                }
            }
            then("update user") {
                coVerify(exactly = 1) {
                    userRepository.save(any())
                }
            }
        }
    }
})

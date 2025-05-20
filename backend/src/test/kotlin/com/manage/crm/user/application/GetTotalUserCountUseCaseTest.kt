package com.manage.crm.user.application

import com.manage.crm.user.application.dto.GetTotalUserCountUseCaseOut
import com.manage.crm.user.application.service.UserService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class GetTotalUserCountUseCaseTest : BehaviorSpec({
    lateinit var userService: UserService
    lateinit var useCase: GetTotalUserCountUseCase

    beforeContainer {
        userService = mockk()
        useCase = GetTotalUserCountUseCase(userService)
    }

    given("GetTotalUserCountUseCase") {
        `when`("execute is called") {
            val expectedCount = 42L
            coEvery { userService.incrementTotalUserCount() } returns expectedCount

            val result = useCase.execute()

            then("should return GetTotalUserCountUseCaseOut with correct count") {
                result shouldBe GetTotalUserCountUseCaseOut(
                    totalCount = expectedCount
                )
            }

            then("call userService to get total user count") {
                coVerify(exactly = 1) { userService.getTotalUserCount() }
            }
        }
    }
})

package com.manage.crm.user.application.service

import com.manage.crm.user.domain.cache.UserCacheManager
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class CacheUserServiceImplTest : FeatureSpec({
    val userCacheManager = mockk<UserCacheManager>()
    val userService = CacheUserServiceImpl(userCacheManager)

    feature("CacheUserServiceImpl#getTotalUserCount") {
        scenario("should return total user count from cache") {
            // given
            val expectedCount = 10L
            coEvery { userCacheManager.totalUserCount() } returns expectedCount

            // when
            val result = userService.getTotalUserCount()

            // then
            result shouldBe expectedCount
            coVerify(exactly = 1) { userCacheManager.totalUserCount() }
        }
    }

    feature("CacheUserServiceImpl#incrementTotalUserCount") {
        scenario("should increment and return new total user count") {
            // given
            val expectedCount = 11L
            coEvery { userCacheManager.incrTotalUserCount() } returns expectedCount

            // when
            val result = userService.incrementTotalUserCount()

            // then
            result shouldBe expectedCount
            coVerify(exactly = 1) { userCacheManager.incrTotalUserCount() }
        }
    }
})

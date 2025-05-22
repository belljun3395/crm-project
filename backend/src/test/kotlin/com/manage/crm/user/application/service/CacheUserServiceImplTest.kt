package com.manage.crm.user.application.service

import com.manage.crm.user.domain.cache.UserCacheManager
import com.manage.crm.user.event.RefreshTotalUsersCommand
import com.manage.crm.user.support.UserEventPublisher
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class CacheUserServiceImplTest : FeatureSpec({
    lateinit var userCacheManager: UserCacheManager
    lateinit var userEventPublisher: UserEventPublisher
    lateinit var userService: CacheUserServiceImpl

    beforeTest {
        userCacheManager = mockk()
        userEventPublisher = mockk(relaxed = true)
        userService = CacheUserServiceImpl(userCacheManager, userEventPublisher)
    }

    feature("CacheUserServiceImpl#getTotalUserCount") {
        scenario("should return total user count from cache") {
            // given
            val expectedCount = 10L
            coEvery { userCacheManager.totalUserCount() } returns expectedCount
            val expectedUpdatedAt = System.currentTimeMillis()
            coEvery { userCacheManager.totalUserCountUpdatedAt() } returns expectedUpdatedAt

            // when
            val result = userService.getTotalUserCount()

            // then
            result shouldBe expectedCount

            coVerify(exactly = 1) { userCacheManager.totalUserCount() }
            coVerify(exactly = 1) { userCacheManager.totalUserCountUpdatedAt() }
        }

        scenario("should publish RefreshTotalUsersCommand when count is old") {
            // given
            val oldCount = 10L
            coEvery { userCacheManager.totalUserCount() } returns oldCount
            val fourHours = 1000L * 60 * 60 * 4
            val expectedUpdatedAt = System.currentTimeMillis() - fourHours
            coEvery { userCacheManager.totalUserCountUpdatedAt() } returns expectedUpdatedAt

            // when
            val result = userService.getTotalUserCount()

            // then
            result shouldBe oldCount
            coVerify(exactly = 1) { userCacheManager.totalUserCount() }
            coVerify(exactly = 1) { userCacheManager.totalUserCountUpdatedAt() }
            coVerify(exactly = 1) { userEventPublisher.publishEvent(any(RefreshTotalUsersCommand::class)) }
        }
    }

    feature("CacheUserServiceImpl#incrementTotalUserCount") {
        scenario("should increment and return new total user count") {
            // given
            val expectedCount = 11L
            coEvery { userCacheManager.incrTotalUserCount() } returns expectedCount
            val expectedUpdatedAt = System.currentTimeMillis()
            coEvery { userCacheManager.updateTotalUserCountUpdateAt() } returns expectedUpdatedAt

            // when
            val result = userService.incrementTotalUserCount()

            // then
            result shouldBe expectedCount

            coVerify(exactly = 1) { userCacheManager.incrTotalUserCount() }
            coVerify(exactly = 1) { userCacheManager.updateTotalUserCountUpdateAt() }
        }
    }
})

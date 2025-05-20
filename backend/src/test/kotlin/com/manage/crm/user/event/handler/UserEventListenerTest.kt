package com.manage.crm.user.event.handler

import com.manage.crm.user.UserEventInvokeSituationTest
import com.manage.crm.user.domain.cache.UserCacheManager
import com.manage.crm.user.domain.cache.UserCacheManager.Companion.TOTAL_USER_COUNT_KEY
import com.manage.crm.user.domain.cache.UserCacheManager.Companion.TOTAL_USER_COUNT_UPDATED_AT_KEY
import com.manage.crm.user.event.RefreshTotalUsersCommand
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mockingDetails
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.modulith.test.Scenario

class UserEventListenerTest(
    private val userCacheManager: UserCacheManager
) : UserEventInvokeSituationTest() {
    private val redisTemplate: ReactiveRedisTemplate<String, Long> = mock()

    @Test
    fun `get total user count when need to refresh count cause count is old`(scenario: Scenario) {
        runTest {
            // given
            val oldCount = 10L
            `when`(redisTemplate.opsForValue().get(TOTAL_USER_COUNT_KEY).awaitSingle())
                .thenReturn(oldCount)

            val fourHours = 1000L * 60 * 60 * 4
            `when`(redisTemplate.opsForValue().get(TOTAL_USER_COUNT_UPDATED_AT_KEY).awaitSingle())
                .thenReturn(System.currentTimeMillis() - fourHours)

            val command = RefreshTotalUsersCommand(oldCount)
            doNothing().`when`(userEventPublisher).publish(command)

            // when
            userCacheManager.totalUserCount()
            `when`(refreshTotalUsersCommandHandler.handle(command)).thenReturn(Unit)

            // then
            val expectedInvocationTime = 1
            scenario.publish(command)
                .andWaitForStateChange(
                    { mockingDetails(refreshTotalUsersCommandHandler).invocations.size }
                )
                .andVerify { invocationTime ->
                    assert(invocationTime == expectedInvocationTime)
                }
        }
    }
}

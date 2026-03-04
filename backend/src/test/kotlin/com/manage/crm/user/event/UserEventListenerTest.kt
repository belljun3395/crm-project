package com.manage.crm.user.event

import com.manage.crm.user.UserEventInvokeSituationTest
import com.manage.crm.user.application.service.UserService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.argThat
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class UserEventListenerTest(
    private val userService: UserService
) : UserEventInvokeSituationTest() {
    @Test
    fun `user service is called and need to refresh user total count cause count is too old`() {
        runTest {
            // given
            val oldCount = 10L
            Mockito.`when`(userCacheManager.totalUserCount()).thenReturn(oldCount)

            val fourHours = 1000L * 60 * 60 * 4
            Mockito.`when`(userCacheManager.totalUserCountUpdatedAt())
                .thenReturn(System.currentTimeMillis() - fourHours)

            val command = RefreshTotalUsersCommand(oldCount)

            // when
            userService.getTotalUserCount()

            // then
            verify(userEventPublisher, times(1)).publishEvent(
                argThat<RefreshTotalUsersCommand> { oldTotalUsers == command.oldTotalUsers }
            )
        }
    }
}

package com.manage.crm.user.event

import com.manage.crm.user.UserEventInvokeSituationTest
import com.manage.crm.user.application.service.UserService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mockingDetails
import org.springframework.modulith.test.Scenario

class UserEventListenerTest(
    private val userService: UserService
) : UserEventInvokeSituationTest() {
    @Test
    fun `user service is called and need to refresh user total count cause count is too old`(scenario: Scenario) {
        runTest {
            // given
            val oldCount = 10L
            Mockito.`when`(userCacheManager.totalUserCount()).thenReturn(oldCount)

            val fourHours = 1000L * 60 * 60 * 4
            Mockito.`when`(userCacheManager.totalUserCountUpdatedAt())
                .thenReturn(System.currentTimeMillis() - fourHours)

            val command = RefreshTotalUsersCommand(oldCount)
            doNothing().`when`(userEventPublisher).publishEvent(command)

            // when
            userService.getTotalUserCount()
            Mockito.`when`(refreshTotalUsersCommandHandler.handle(command)).thenReturn(Unit)

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

package com.manage.crm.user

import com.manage.crm.infrastructure.scheduler.provider.AwsSchedulerService
import com.manage.crm.user.event.handler.RefreshTotalUsersCommandHandler
import com.manage.crm.user.support.UserEventPublisher
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores::class)
abstract class UserEventInvokeSituationTest : UserModuleTestTemplate() {
    @MockitoBean
    lateinit var awsSchedulerService: AwsSchedulerService

    // ----------------- Common -----------------
    @MockitoBean
    lateinit var userEventPublisher: UserEventPublisher

//    @MockitoBean
//    lateinit var redisTemplate: ReactiveRedisTemplate<String, Any>

    // ----------------- RefreshTotalUsersCommandHandlerTest -----------------
    @MockitoBean
    lateinit var refreshTotalUsersCommandHandler: RefreshTotalUsersCommandHandler
}

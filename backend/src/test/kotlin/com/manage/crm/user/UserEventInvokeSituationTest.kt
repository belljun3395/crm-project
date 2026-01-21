package com.manage.crm.user

import com.manage.crm.user.domain.cache.UserCacheManager
import com.manage.crm.user.event.handler.RefreshTotalUsersCommandHandler
import com.manage.crm.user.support.UserEventPublisher
import com.manage.crm.webhook.application.WebhookEventListener
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator
import org.springframework.test.context.bean.override.mockito.MockitoBean

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores::class)
abstract class UserEventInvokeSituationTest : UserModuleTestTemplate() {
    // ----------------- Common -----------------
    @MockitoBean
    lateinit var userEventPublisher: UserEventPublisher

    // ----------------- RefreshTotalUsersCommandHandlerTest -----------------
    @MockitoBean
    lateinit var refreshTotalUsersCommandHandler: RefreshTotalUsersCommandHandler

    @MockitoBean
    lateinit var userCacheManager: UserCacheManager

    // ----------------- WebhookEventListener -----------------
    @MockitoBean
    lateinit var webhookEventListener: WebhookEventListener
}

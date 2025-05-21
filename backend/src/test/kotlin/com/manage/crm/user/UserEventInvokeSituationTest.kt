package com.manage.crm.user

import com.manage.crm.user.support.UserEventPublisher
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator
import org.springframework.test.context.bean.override.mockito.MockitoBean

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores::class)
abstract class UserEventInvokeSituationTest : UserModuleTestTemplate() {
    // ----------------- Common -----------------
    @MockitoBean
    lateinit var userEventPublisher: UserEventPublisher
}

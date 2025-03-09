package com.manage.crm.email

import com.manage.crm.email.application.service.NonVariablesMailService
import com.manage.crm.email.event.schedule.handler.CancelScheduledEventHandler
import com.manage.crm.email.event.send.handler.EmailClickEventHandler
import com.manage.crm.email.event.send.handler.EmailDeliveryDelayEventHandler
import com.manage.crm.email.event.send.handler.EmailDeliveryEventHandler
import com.manage.crm.email.event.send.handler.EmailOpenEventHandler
import com.manage.crm.email.event.send.handler.EmailSentEventHandler
import com.manage.crm.email.event.send.notification.handler.NotificationEmailSendTimeOutEventHandler
import com.manage.crm.email.event.send.notification.handler.NotificationEmailSendTimeOutInvokeEventHandler
import com.manage.crm.email.event.template.handler.PostEmailTemplateEventHandler
import com.manage.crm.infrastructure.scheduler.provider.AwsSchedulerService
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator
import org.springframework.test.context.bean.override.mockito.MockitoBean

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores::class)
abstract class MailEventInvokeSituationTest : EmailModuleTestTemplate() {
    // ----------------- CancelScheduledEventHandlerTest -----------------
    @MockitoBean
    lateinit var awsSchedulerService: AwsSchedulerService

    @MockitoBean
    lateinit var cancelScheduledEventHandler: CancelScheduledEventHandler

    // ----------------- NotificationEmailSendTimeOutEventListenerTest -----------------
    @MockitoBean
    lateinit var notificationEmailSendTimeOutEventHandler: NotificationEmailSendTimeOutEventHandler

    @MockitoBean
    lateinit var notificationEmailSendTimeOutInvokeEventHandler: NotificationEmailSendTimeOutInvokeEventHandler

    // ----------------- EmailSendEventListenerInvokeSituation -----------------
    @MockitoBean
    lateinit var nonVariablesMailService: NonVariablesMailService

    @MockitoBean
    lateinit var emailSentEventHandler: EmailSentEventHandler

    @MockitoBean
    lateinit var emailOpenEventHandler: EmailOpenEventHandler

    @MockitoBean
    lateinit var emailDeliveryEventHandler: EmailDeliveryEventHandler

    @MockitoBean
    lateinit var emailDeliveryDelayEventHandler: EmailDeliveryDelayEventHandler

    @MockitoBean
    lateinit var emailClickEventHandler: EmailClickEventHandler

    // ----------------- EventTemplateTransactionListenerInvokeSituation -----------------
    @MockitoBean
    lateinit var postEmailTemplateEventHandler: PostEmailTemplateEventHandler
}

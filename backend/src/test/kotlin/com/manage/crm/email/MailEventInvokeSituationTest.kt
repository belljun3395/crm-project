package com.manage.crm.email

import com.manage.crm.email.application.service.MailServiceImpl
import com.manage.crm.email.event.schedule.handler.CancelScheduledEventHandler
import com.manage.crm.email.event.send.handler.EmailClickEventHandler
import com.manage.crm.email.event.send.handler.EmailDeliveryDelayEventHandler
import com.manage.crm.email.event.send.handler.EmailDeliveryEventHandler
import com.manage.crm.email.event.send.handler.EmailOpenEventHandler
import com.manage.crm.email.event.send.handler.EmailSentEventHandler
import com.manage.crm.email.event.send.notification.handler.NotificationEmailSendTimeOutEventHandler
import com.manage.crm.email.event.send.notification.handler.NotificationEmailSendTimeOutInvokeEventHandler
import com.manage.crm.email.event.template.handler.PostEmailTemplateEventHandler
import com.manage.crm.email.support.EmailEventPublisher
import com.manage.crm.infrastructure.message.config.MessageConfig
import com.manage.crm.infrastructure.scheduler.handler.ScheduledTaskHandler
import com.manage.crm.infrastructure.scheduler.provider.AwsSchedulerService
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator
import org.springframework.test.context.bean.override.mockito.MockitoBean
import software.amazon.awssdk.services.sqs.SqsAsyncClient

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores::class)
abstract class MailEventInvokeSituationTest : EmailModuleTestTemplate() {
    // ----------------- Common -----------------
    @MockitoBean(enforceOverride = false)
    lateinit var emailEventPublisher: EmailEventPublisher

    @MockitoBean(enforceOverride = false)
    lateinit var scheduledTaskHandler: ScheduledTaskHandler

    @MockitoBean(name = MessageConfig.SQS_ASYNC_CLIENT, enforceOverride = false)
    lateinit var sqsAsyncClient: SqsAsyncClient

    @MockitoBean(name = MessageConfig.SQS_TEMPLATE, enforceOverride = false)
    lateinit var sqsTemplate: SqsTemplate

    @Suppress("UNCHECKED_CAST")
    @MockitoBean(name = MessageConfig.SQS_LISTENER_CONTAINER_FACTORY, enforceOverride = false)
    lateinit var sqsListenerContainerFactory: SqsMessageListenerContainerFactory<Any>

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
    lateinit var mailServiceImpl: MailServiceImpl

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

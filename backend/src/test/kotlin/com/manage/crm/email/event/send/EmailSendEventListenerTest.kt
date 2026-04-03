package com.manage.crm.email.event.send

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.SendEmailInDto
import com.manage.crm.email.application.dto.SendEmailOutDto
import com.manage.crm.email.application.service.MailService
import com.manage.crm.email.domain.vo.EmailProviderType
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.email.event.relay.EmailTrackingEvent
import com.manage.crm.email.event.relay.EmailTrackingEventMapper
import com.manage.crm.email.event.relay.EmailTrackingEventType
import com.manage.crm.email.support.EmailEventPublisher
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Qualifier
import java.time.LocalDateTime

class EmailSendEventListenerTest(
    @Qualifier("mailServicePostEventProcessor")
    val mailService: MailService,
) : MailEventInvokeSituationTest() {
    private val mapper = EmailTrackingEventMapper()
    private val mockPublisher = mock(EmailEventPublisher::class.java)

    init {
        given("mail service") {
            then("after mail service is called") {
                val sendEmailInDto =
                    SendEmailInDto(
                        to = "example@example.com",
                        subject = "subject",
                        template = "template",
                        content = NonContent(),
                        emailBody = "body",
                        destination = "example@example.com",
                        eventType = SentEmailStatus.SEND,
                    )
                `when`(mailServiceImpl.send(sendEmailInDto.emailArgs)).thenReturn("messageId")

                `when`(mailServiceImpl.send(sendEmailInDto)).thenReturn(
                    SendEmailOutDto(
                        userId = 1,
                        emailBody = "body",
                        messageId = "messageId",
                        destination = "example@example.com",
                        provider = EmailProviderType.SMTP,
                    ),
                )

                val event =
                    EmailSentEvent(
                        userId = 1,
                        emailBody = "body",
                        messageId = "messageId",
                        destination = "example@example.com",
                        provider = EmailProviderType.SMTP,
                    )
                doNothing().`when`(emailEventPublisher).publishEvent(event)

                mailService.send(sendEmailInDto)

                verify(emailEventPublisher, times(1)).publishEvent(any<EmailSentEvent>())
            }
        }

        given("email tracking event mapper") {
            val messageId = "messageId"
            val email = "example@example.com"
            val timestamp = LocalDateTime.now()

            then("map open tracking event to domain event") {
                val trackingEvent =
                    EmailTrackingEvent(
                        eventType = EmailTrackingEventType.OPEN,
                        messageId = messageId,
                        destination = email,
                        occurredAt = timestamp,
                        provider = EmailProviderType.WEBHOOK,
                    )
                val domainEvent = mapper.toDomainEvent(trackingEvent)

                doNothing().`when`(mockPublisher).publishEvent(domainEvent!!)
                mockPublisher.publishEvent(domainEvent!!)

                verify(mockPublisher, times(1)).publishEvent(any<EmailOpenEvent>())
            }

            then("map delivery tracking event to domain event") {
                val trackingEvent =
                    EmailTrackingEvent(
                        eventType = EmailTrackingEventType.DELIVERY,
                        messageId = messageId,
                        destination = email,
                        occurredAt = timestamp,
                        provider = EmailProviderType.WEBHOOK,
                    )
                val domainEvent = mapper.toDomainEvent(trackingEvent)

                doNothing().`when`(mockPublisher).publishEvent(domainEvent!!)
                mockPublisher.publishEvent(domainEvent!!)

                verify(mockPublisher, times(1)).publishEvent(any<EmailDeliveryEvent>())
            }

            then("map delivery delay tracking event to domain event") {
                val trackingEvent =
                    EmailTrackingEvent(
                        eventType = EmailTrackingEventType.DELIVERY_DELAY,
                        messageId = messageId,
                        destination = email,
                        occurredAt = timestamp,
                        provider = EmailProviderType.WEBHOOK,
                    )
                val domainEvent = mapper.toDomainEvent(trackingEvent)

                doNothing().`when`(mockPublisher).publishEvent(domainEvent!!)
                mockPublisher.publishEvent(domainEvent!!)

                verify(mockPublisher, times(1)).publishEvent(any<EmailDeliveryDelayEvent>())
            }

            then("map click tracking event to domain event") {
                val trackingEvent =
                    EmailTrackingEvent(
                        eventType = EmailTrackingEventType.CLICK,
                        messageId = messageId,
                        destination = email,
                        occurredAt = timestamp,
                        provider = EmailProviderType.WEBHOOK,
                    )
                val domainEvent = mapper.toDomainEvent(trackingEvent)

                doNothing().`when`(mockPublisher).publishEvent(domainEvent!!)
                mockPublisher.publishEvent(domainEvent!!)

                verify(mockPublisher, times(1)).publishEvent(any<EmailClickEvent>())
            }

            then("bounce event should return null") {
                val trackingEvent =
                    EmailTrackingEvent(
                        eventType = EmailTrackingEventType.BOUNCE,
                        messageId = messageId,
                        destination = email,
                        occurredAt = timestamp,
                        provider = EmailProviderType.WEBHOOK,
                    )
                val domainEvent = mapper.toDomainEvent(trackingEvent)
                assert(domainEvent == null)
            }
        }
    }
}

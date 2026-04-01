package com.manage.crm.email.event.send

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.SendEmailInDto
import com.manage.crm.email.application.dto.SendEmailOutDto
import com.manage.crm.email.application.service.MailService
import com.manage.crm.email.domain.vo.EmailProviderType
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.email.event.relay.aws.SesEmailEventFactory
import com.manage.crm.email.event.relay.aws.SesMessageReverseRelay
import com.manage.crm.email.event.relay.aws.mapper.SesMessageMapper
import com.manage.crm.email.support.EmailEventPublisher
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Qualifier
import java.time.ZonedDateTime

fun getMessage(
    status: SentEmailStatus,
    email: String,
    timeStamp: ZonedDateTime,
    messageId: String,
): String =
    """
    {
        "Type" : "Notification",
        "MessageId" : "$messageId",
        "TopicArn" : "arn:aws:sns:us-west-2:123456789012:MyTopic",
        "Subject" : "test",
        "Message" : "{ \"eventType\": \"${status.name}\", \"mail\": { \"messageId\": \"$messageId\", \"destination\": [\"$email\"], \"timestamp\": \"$timeStamp\" } }",
        "Timestamp" : "$timeStamp",
        "SignatureVersion" : "1",
        "Signature" : "EXAMPLE",
        "SigningCertURL" : "EXAMPLE",
        "UnsubscribeURL" : "EXAMPLE"
    }
    """.trimIndent()

class EmailSendEventListenerTest(
    @Qualifier("mailServicePostEventProcessor")
    val mailService: MailService,
    eventMessageMapper: SesMessageMapper,
) : MailEventInvokeSituationTest() {
    private val sesMessageReverseRelayEmailEventPublisher = mock(EmailEventPublisher::class.java)
    private val sesEmailEventFactory = SesEmailEventFactory()
    private var sesMessageReverseRelay: SesMessageReverseRelay =
        SesMessageReverseRelay(
            sesMessageReverseRelayEmailEventPublisher,
            eventMessageMapper,
            sesEmailEventFactory,
        )

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
                        provider = EmailProviderType.AWS,
                    ),
                )

                val event =
                    EmailSentEvent(
                        userId = 1,
                        emailBody = "body",
                        messageId = "messageId",
                        destination = "example@example.com",
                        provider = EmailProviderType.AWS,
                    )
                doNothing().`when`(emailEventPublisher).publishEvent(event)

                mailService.send(sendEmailInDto)

                verify(emailEventPublisher, times(1)).publishEvent(any<EmailSentEvent>())
            }
        }

        given("ses message reverse relay") {
            then("receive open message from ses") {
                val zoneTime = ZonedDateTime.now()
                val timeStamp = zoneTime.toLocalDateTime()
                val email = "example@example.com"
                val messageId = "messageId"
                val message = getMessage(SentEmailStatus.OPEN, email, zoneTime, messageId)
                val acknowledgement = mock(Acknowledgement::class.java)
                doNothing().`when`(acknowledgement).acknowledge()

                val event =
                    EmailOpenEvent(
                        messageId = messageId,
                        destination = email,
                        timestamp = timeStamp,
                        provider = EmailProviderType.AWS,
                    )
                doNothing().`when`(sesMessageReverseRelayEmailEventPublisher).publishEvent(event)

                sesMessageReverseRelay.onMessage(message, acknowledgement)

                verify(sesMessageReverseRelayEmailEventPublisher, times(1)).publishEvent(any<EmailOpenEvent>())
            }

            then("receive delivery message from ses") {
                val zoneTime = ZonedDateTime.now()
                val timeStamp = zoneTime.toLocalDateTime()
                val email = "example@example.com"
                val messageId = "messageId"
                val message = getMessage(SentEmailStatus.DELIVERY, email, zoneTime, messageId)
                val acknowledgement = mock(Acknowledgement::class.java)
                doNothing().`when`(acknowledgement).acknowledge()

                val event =
                    EmailDeliveryEvent(
                        messageId = messageId,
                        destination = email,
                        timestamp = timeStamp,
                        provider = EmailProviderType.AWS,
                    )
                doNothing().`when`(sesMessageReverseRelayEmailEventPublisher).publishEvent(event)

                sesMessageReverseRelay.onMessage(message, acknowledgement)

                verify(sesMessageReverseRelayEmailEventPublisher, times(1)).publishEvent(any<EmailDeliveryEvent>())
            }

            then("receive delivery delay message from ses") {
                val zoneTime = ZonedDateTime.now()
                val timeStamp = zoneTime.toLocalDateTime()
                val email = "example@example.com"
                val messageId = "messageId"
                val message = getMessage(SentEmailStatus.DELIVERYDELAY, email, zoneTime, messageId)
                val acknowledgement = mock(Acknowledgement::class.java)
                doNothing().`when`(acknowledgement).acknowledge()

                val event =
                    EmailDeliveryDelayEvent(
                        messageId = messageId,
                        destination = email,
                        timestamp = timeStamp,
                        provider = EmailProviderType.AWS,
                    )
                doNothing().`when`(sesMessageReverseRelayEmailEventPublisher).publishEvent(event)

                sesMessageReverseRelay.onMessage(message, acknowledgement)

                verify(sesMessageReverseRelayEmailEventPublisher, times(1)).publishEvent(any<EmailDeliveryDelayEvent>())
            }

            then("receive click message from ses") {
                val zoneTime = ZonedDateTime.now()
                val timeStamp = zoneTime.toLocalDateTime()
                val email = "example@example.com"
                val messageId = "messageId"
                val message = getMessage(SentEmailStatus.CLICK, email, zoneTime, messageId)
                val acknowledgement = mock(Acknowledgement::class.java)
                doNothing().`when`(acknowledgement).acknowledge()

                val event =
                    EmailClickEvent(
                        messageId = messageId,
                        destination = email,
                        timestamp = timeStamp,
                        provider = EmailProviderType.AWS,
                    )
                doNothing().`when`(sesMessageReverseRelayEmailEventPublisher).publishEvent(event)

                sesMessageReverseRelay.onMessage(message, acknowledgement)

                verify(sesMessageReverseRelayEmailEventPublisher, times(1)).publishEvent(any<EmailClickEvent>())
            }
        }
    }
}

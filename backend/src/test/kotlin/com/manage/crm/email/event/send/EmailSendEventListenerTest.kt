package com.manage.crm.email.event.send

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.SendEmailInDto
import com.manage.crm.email.application.dto.SendEmailOutDto
import com.manage.crm.email.application.service.MailService
import com.manage.crm.email.domain.vo.EmailProviderType
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.email.event.relay.aws.SesMessageReverseRelay
import com.manage.crm.email.event.relay.aws.mapper.SesMessageMapper
import com.manage.crm.email.support.EmailEventPublisher
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.modulith.test.Scenario
import java.time.ZonedDateTime
import kotlin.test.assertEquals

fun getMessage(
    status: SentEmailStatus,
    email: String,
    timeStamp: ZonedDateTime,
    messageId: String
): String {
    return """
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
}

class EmailSendEventListenerTest(
    @Qualifier("mailServicePostEventProcessor")
    val mailService: MailService,
    eventMessageMapper: SesMessageMapper
) : MailEventInvokeSituationTest() {

    private val sesMessageReverseRelayEmailEventPublisher = mock(EmailEventPublisher::class.java)
    private var sesMessageReverseRelay: SesMessageReverseRelay =
        SesMessageReverseRelay(sesMessageReverseRelayEmailEventPublisher, eventMessageMapper)

    @Test
    fun `after mail service is called`(scenario: Scenario) {
        runTest {
            // given
            val sendEmailInDto = SendEmailInDto(
                to = "example@example.com",
                subject = "subject",
                template = "template",
                content = NonContent(),
                emailBody = "body",
                destination = "example@example.com",
                eventType = SentEmailStatus.SEND
            )
            `when`(mailServiceImpl.send(sendEmailInDto.emailArgs)).thenReturn("messageId")

            `when`(mailServiceImpl.send(sendEmailInDto)).thenReturn(
                SendEmailOutDto(
                    userId = 1,
                    emailBody = "body",
                    messageId = "messageId",
                    destination = "example@example.com",
                    provider = EmailProviderType.AWS
                )
            )

            val event = EmailSentEvent(
                userId = 1,
                emailBody = "body",
                messageId = "messageId",
                destination = "example@example.com",
                provider = EmailProviderType.AWS
            )
            doNothing().`when`(emailEventPublisher).publishEvent(event)

            // when
            mailService.send(sendEmailInDto)

            `when`(emailSentEventHandler.handle(event)).thenReturn(Unit)

            // then
            val expectedInvocationTime = 1
            scenario.publish(event)
                .andWaitForStateChange(
                    { mockingDetails(emailSentEventHandler).invocations.size },
                    { mockingDetails(emailSentEventHandler).invocations.size == expectedInvocationTime }
                )
                .andVerify { invocationTime ->
                    assertEquals(invocationTime, expectedInvocationTime)
                }
        }
    }

    @Test
    fun `receive open message from ses`(scenario: Scenario) {
        runTest {
            // given
            val zoneTime = ZonedDateTime.now()
            val timeStamp = zoneTime.toLocalDateTime()
            val email = "example@example.com"
            val messageId = "messageId"
            val message = getMessage(SentEmailStatus.OPEN, email, zoneTime, messageId)
            val acknowledgement = mock(Acknowledgement::class.java)
            doNothing().`when`(acknowledgement).acknowledge()

            val event = EmailOpenEvent(
                messageId = messageId,
                destination = email,
                timestamp = timeStamp,
                provider = EmailProviderType.AWS
            )
            doNothing().`when`(sesMessageReverseRelayEmailEventPublisher).publishEvent(event)

            // when
            sesMessageReverseRelay.onMessage(message, acknowledgement)

            `when`(emailOpenEventHandler.handle(event)).thenReturn(Unit)

            // then
            val expectedInvocationTime = 1
            scenario.publish(event)
                .andWaitForStateChange(
                    { mockingDetails(emailOpenEventHandler).invocations.size },
                    { mockingDetails(emailOpenEventHandler).invocations.size == expectedInvocationTime }
                )
                .andVerify { invocationTime ->
                    assertEquals(invocationTime, expectedInvocationTime)
                }
        }
    }

    @Test
    fun `receive delivery message from ses`(scenario: Scenario) {
        runTest {
            // given
            val zoneTime = ZonedDateTime.now()
            val timeStamp = zoneTime.toLocalDateTime()
            val email = "example@example.com"
            val messageId = "messageId"
            val message = getMessage(SentEmailStatus.DELIVERY, email, zoneTime, messageId)
            val acknowledgement = mock(Acknowledgement::class.java)
            doNothing().`when`(acknowledgement).acknowledge()

            val event = EmailDeliveryEvent(
                messageId = messageId,
                destination = email,
                timestamp = timeStamp,
                provider = EmailProviderType.AWS
            )
            doNothing().`when`(sesMessageReverseRelayEmailEventPublisher).publishEvent(event)

            // when
            sesMessageReverseRelay.onMessage(message, acknowledgement)

            `when`(emailDeliveryEventHandler.handle(event)).thenReturn(Unit)

            // then
            val expectedInvocationTime = 1
            scenario.publish(event)
                .andWaitForStateChange(
                    { mockingDetails(emailDeliveryEventHandler).invocations.size },
                    { mockingDetails(emailDeliveryEventHandler).invocations.size == expectedInvocationTime }
                )
                .andVerify { invocationTime ->
                    assertEquals(invocationTime, expectedInvocationTime)
                }
        }
    }

    @Test
    fun `receive delivery delay message from ses`(scenario: Scenario) {
        runTest {
            // given
            val zoneTime = ZonedDateTime.now()
            val timeStamp = zoneTime.toLocalDateTime()
            val email = "example@example.com"
            val messageId = "messageId"
            val message = getMessage(SentEmailStatus.DELIVERYDELAY, email, zoneTime, messageId)
            val acknowledgement = mock(Acknowledgement::class.java)
            doNothing().`when`(acknowledgement).acknowledge()

            val event = EmailDeliveryDelayEvent(
                messageId = messageId,
                destination = email,
                timestamp = timeStamp,
                provider = EmailProviderType.AWS
            )
            doNothing().`when`(sesMessageReverseRelayEmailEventPublisher).publishEvent(event)

            // when
            sesMessageReverseRelay.onMessage(message, acknowledgement)

            `when`(emailDeliveryDelayEventHandler.handle(event)).thenReturn(Unit)

            // then
            val expectedInvocationTime = 1
            scenario.publish(event)
                .andWaitForStateChange(
                    { mockingDetails(emailDeliveryDelayEventHandler).invocations.size },
                    { mockingDetails(emailDeliveryDelayEventHandler).invocations.size == expectedInvocationTime }
                )
                .andVerify { invocationTime ->
                    assertEquals(invocationTime, expectedInvocationTime)
                }
        }
    }

    @Test
    fun `receive click message from ses`(scenario: Scenario) {
        runTest {
            // given
            val zoneTime = ZonedDateTime.now()
            val timeStamp = zoneTime.toLocalDateTime()
            val email = "example@example.com"
            val messageId = "messageId"
            val message = getMessage(SentEmailStatus.CLICK, email, zoneTime, messageId)
            val acknowledgement = mock(Acknowledgement::class.java)
            doNothing().`when`(acknowledgement).acknowledge()

            val event = EmailClickEvent(
                messageId = messageId,
                destination = email,
                timestamp = timeStamp,
                provider = EmailProviderType.AWS
            )
            doNothing().`when`(sesMessageReverseRelayEmailEventPublisher).publishEvent(event)

            // when
            sesMessageReverseRelay.onMessage(message, acknowledgement)

            `when`(emailClickEventHandler.handle(event)).thenReturn(Unit)

            // then
            val expectedInvocationTime = 1
            scenario.publish(event)
                .andWaitForStateChange(
                    { mockingDetails(emailClickEventHandler).invocations.size },
                    { mockingDetails(emailClickEventHandler).invocations.size == expectedInvocationTime }
                )
                .andVerify { invocationTime ->
                    assertEquals(invocationTime, expectedInvocationTime)
                }
        }
    }
}

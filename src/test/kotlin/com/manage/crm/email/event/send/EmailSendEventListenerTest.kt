package com.manage.crm.email.event.send

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.SendEmailDto
import com.manage.crm.email.domain.vo.EmailProviderType
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.email.event.relay.aws.SesMessageReverseRelay
import com.manage.crm.email.event.relay.aws.mapper.SesMessageMapper
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import org.springframework.modulith.test.Scenario
import java.time.ZonedDateTime

fun getMessage(status: SentEmailStatus, email: String, timeStamp: ZonedDateTime, messageId: String): String {
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
    applicationEventPublisher: ApplicationEventPublisher,
    eventMessageMapper: SesMessageMapper
) : MailEventInvokeSituationTest() {

    private var sesMessageReverseRelay: SesMessageReverseRelay =
        SesMessageReverseRelay(applicationEventPublisher, eventMessageMapper)

    @Test
    fun `after non-variable mail service is called`(scenario: Scenario) {
        runTest {
            // given
            val sendEmailDto = SendEmailDto(
                to = "example@example.com",
                subject = "subject",
                template = "template",
                content = NonContent(),
                emailBody = "body",
                destination = "example@example.com",
                eventType = SentEmailStatus.SEND
            )
            `when`(nonVariablesMailService.send(sendEmailDto.emailArgs)).thenReturn("messageId")

            // when
            run {
                nonVariablesMailService.send(sendEmailDto)

                val event = EmailSentEvent(
                    userId = 1,
                    emailBody = "body",
                    messageId = "messageId",
                    destination = "example@example.com",
                    provider = EmailProviderType.AWS
                )
                `when`(emailSentEventHandler.handle(event)).thenReturn(Unit)

                // then
                run {
                    scenario.publish(event)
                        .andWaitForEventOfType(EmailSentEvent::class.java)
                        .toArriveAndAssert { _, _ ->
                            runBlocking {
                                verify(emailSentEventHandler, times(1)).handle(event)
                            }
                        }
                }
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
            val acknowledgement = Mockito.mock(Acknowledgement::class.java)
            doNothing().`when`(acknowledgement).acknowledge()

            // when
            run {
                sesMessageReverseRelay.onMessage(message, acknowledgement)

                val event = EmailOpenEvent(
                    messageId = messageId,
                    destination = email,
                    timestamp = timeStamp,
                    provider = EmailProviderType.AWS
                )

                // then
                run {
                    scenario.publish(event)
                        .andWaitForEventOfType(EmailOpenEvent::class.java)
                        .toArriveAndAssert { _, _ ->
                            runBlocking {
                                verify(emailOpenEventHandler, times(1)).handle(event)
                            }
                        }
                }
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
            val acknowledgement = Mockito.mock(Acknowledgement::class.java)
            doNothing().`when`(acknowledgement).acknowledge()

            // when
            run {
                sesMessageReverseRelay.onMessage(message, acknowledgement)

                val event = EmailDeliveryEvent(
                    messageId = messageId,
                    destination = email,
                    timestamp = timeStamp,
                    provider = EmailProviderType.AWS
                )

                // then
                run {
                    scenario.publish(event)
                        .andWaitForEventOfType(EmailDeliveryEvent::class.java)
                        .toArriveAndAssert { _, _ ->
                            runBlocking {
                                verify(emailDeliveryEventHandler, times(1)).handle(event)
                            }
                        }
                }
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
            val acknowledgement = Mockito.mock(Acknowledgement::class.java)
            doNothing().`when`(acknowledgement).acknowledge()

            // when
            run {
                sesMessageReverseRelay.onMessage(message, acknowledgement)

                val event = EmailDeliveryDelayEvent(
                    messageId = messageId,
                    destination = email,
                    timestamp = timeStamp,
                    provider = EmailProviderType.AWS
                )

                // then
                run {
                    scenario.publish(event)
                        .andWaitForEventOfType(EmailDeliveryDelayEvent::class.java)
                        .toArriveAndAssert { _, _ ->
                            runBlocking {
                                verify(emailDeliveryDelayEventHandler, times(1)).handle(event)
                            }
                        }
                }
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
            val acknowledgement = Mockito.mock(Acknowledgement::class.java)
            doNothing().`when`(acknowledgement).acknowledge()

            // when
            run {
                sesMessageReverseRelay.onMessage(message, acknowledgement)

                val event = EmailClickEvent(
                    messageId = messageId,
                    destination = email,
                    timestamp = timeStamp,
                    provider = EmailProviderType.AWS
                )
                // then
                run {
                    scenario.publish(event)
                        .andWaitForEventOfType(EmailClickEvent::class.java)
                        .toArriveAndAssert { _, _ ->
                            runBlocking {
                                verify(emailClickEventHandler, times(1)).handle(event)
                            }
                        }
                }
            }
        }
    }
}

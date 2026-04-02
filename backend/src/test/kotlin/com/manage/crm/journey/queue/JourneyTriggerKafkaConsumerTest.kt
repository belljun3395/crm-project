package com.manage.crm.journey.queue

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.springframework.kafka.support.Acknowledgment

class JourneyTriggerKafkaConsumerTest :
    BehaviorSpec({
        lateinit var processor: JourneyTriggerQueueProcessor
        lateinit var consumer: JourneyTriggerKafkaConsumer
        lateinit var acknowledgment: Acknowledgment

        beforeEach {
            processor = mockk()
            acknowledgment = mockk()
            consumer = JourneyTriggerKafkaConsumer(processor)
            coEvery { processor.process(any()) } returns Unit
            every { acknowledgment.acknowledge() } returns Unit
        }

        given("consume success") {
            `when`("processor handles message") {
                then("it acknowledges once") {
                    val message = JourneyTriggerQueueMessage(triggerType = JourneyTriggerQueueType.SEGMENT_CONTEXT)

                    consumer.consume(message, acknowledgment)

                    coVerify(exactly = 1) { processor.process(message) }
                    io.mockk.verify(exactly = 1) { acknowledgment.acknowledge() }
                }
            }
        }

        given("consume failure") {
            `when`("processor throws") {
                then("it swallows the error and still acknowledges to prevent infinite retry") {
                    val message = JourneyTriggerQueueMessage(triggerType = JourneyTriggerQueueType.SEGMENT_CONTEXT)
                    coEvery { processor.process(message) } throws IllegalStateException("boom")

                    consumer.consume(message, acknowledgment)

                    coVerify(exactly = 1) { processor.process(message) }
                    io.mockk.verify(exactly = 1) { acknowledgment.acknowledge() }
                }
            }
        }
    })

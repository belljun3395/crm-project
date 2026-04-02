package com.manage.crm.journey.domain

import java.time.LocalDateTime
import kotlin.random.Random

class JourneyStepFixtures private constructor() {
    private var id: Long? = null
    private var journeyId: Long = -1L
    private var stepOrder: Int = 1
    private var stepType: String = "ACTION"
    private var channel: String? = "EMAIL"
    private var destination: String? = "test@example.com"
    private var subject: String? = null
    private var body: String? = "Hello, \${user.name}"
    private var variablesJson: String? = null
    private var delayMillis: Long? = null
    private var conditionExpression: String? = null
    private var retryCount: Int = 0
    private var createdAt: LocalDateTime? = null

    fun withId(id: Long?) = apply { this.id = id }

    fun withJourneyId(journeyId: Long) = apply { this.journeyId = journeyId }

    fun withStepOrder(stepOrder: Int) = apply { this.stepOrder = stepOrder }

    fun withStepType(stepType: String) = apply { this.stepType = stepType }

    fun withChannel(channel: String?) = apply { this.channel = channel }

    fun withDestination(destination: String?) = apply { this.destination = destination }

    fun withSubject(subject: String?) = apply { this.subject = subject }

    fun withVariablesJson(variablesJson: String?) = apply { this.variablesJson = variablesJson }

    fun withBody(body: String?) = apply { this.body = body }

    fun withDelayMillis(delayMillis: Long?) = apply { this.delayMillis = delayMillis }

    fun withConditionExpression(conditionExpression: String?) = apply { this.conditionExpression = conditionExpression }

    fun withRetryCount(retryCount: Int) = apply { this.retryCount = retryCount }

    fun withCreatedAt(createdAt: LocalDateTime?) = apply { this.createdAt = createdAt }

    fun build(): JourneyStep =
        JourneyStep(
            id = id,
            journeyId = journeyId,
            stepOrder = stepOrder,
            stepType = stepType,
            channel = channel,
            destination = destination,
            subject = subject,
            body = body,
            variablesJson = variablesJson,
            delayMillis = delayMillis,
            conditionExpression = conditionExpression,
            retryCount = retryCount,
            createdAt = createdAt,
        )

    companion object {
        fun aJourneyStep() = JourneyStepFixtures()

        fun giveMeOne(): JourneyStepFixtures {
            val id = Random.nextLong(1, 101)
            val journeyId = Random.nextLong(1, 101)
            return aJourneyStep()
                .withId(id)
                .withJourneyId(journeyId)
        }

        fun anActionStep(journeyId: Long = 1L): JourneyStepFixtures =
            giveMeOne()
                .withJourneyId(journeyId)
                .withStepType("ACTION")
                .withChannel("EMAIL")
                .withDestination("test@example.com")
                .withBody("Hello")

        fun aDelayStep(journeyId: Long = 1L): JourneyStepFixtures =
            giveMeOne()
                .withJourneyId(journeyId)
                .withStepType("DELAY")
                .withChannel(null)
                .withDestination(null)
                .withBody(null)
                .withDelayMillis(1000L)

        fun aBranchStep(journeyId: Long = 1L): JourneyStepFixtures =
            giveMeOne()
                .withJourneyId(journeyId)
                .withStepType("BRANCH")
                .withChannel(null)
                .withDestination(null)
                .withBody(null)
                .withConditionExpression("event.name == 'SIGNUP'")
    }
}

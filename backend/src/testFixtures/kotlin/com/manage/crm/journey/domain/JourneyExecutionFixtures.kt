package com.manage.crm.journey.domain

import java.time.LocalDateTime
import kotlin.random.Random

class JourneyExecutionFixtures private constructor() {
    private var id: Long? = null
    private var journeyId: Long = -1L
    private var eventId: Long = -1L
    private var userId: Long = -1L
    private var status: String = "RUNNING"
    private var currentStepOrder: Int = 0
    private var lastError: String? = null
    private var triggerKey: String = "default-trigger-key"
    private var startedAt: LocalDateTime = LocalDateTime.now()
    private var completedAt: LocalDateTime? = null
    private var createdAt: LocalDateTime? = null
    private var updatedAt: LocalDateTime? = null

    fun withId(id: Long?) = apply { this.id = id }

    fun withJourneyId(journeyId: Long) = apply { this.journeyId = journeyId }

    fun withEventId(eventId: Long) = apply { this.eventId = eventId }

    fun withUserId(userId: Long) = apply { this.userId = userId }

    fun withStatus(status: String) = apply { this.status = status }

    fun withCurrentStepOrder(currentStepOrder: Int) = apply { this.currentStepOrder = currentStepOrder }

    fun withLastError(lastError: String?) = apply { this.lastError = lastError }

    fun withTriggerKey(triggerKey: String) = apply { this.triggerKey = triggerKey }

    fun withStartedAt(startedAt: LocalDateTime) = apply { this.startedAt = startedAt }

    fun withCompletedAt(completedAt: LocalDateTime?) = apply { this.completedAt = completedAt }

    fun withCreatedAt(createdAt: LocalDateTime?) = apply { this.createdAt = createdAt }

    fun withUpdatedAt(updatedAt: LocalDateTime?) = apply { this.updatedAt = updatedAt }

    fun build(): JourneyExecution =
        JourneyExecution(
            id = id,
            journeyId = journeyId,
            eventId = eventId,
            userId = userId,
            status = status,
            currentStepOrder = currentStepOrder,
            lastError = lastError,
            triggerKey = triggerKey,
            startedAt = startedAt,
            completedAt = completedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun aJourneyExecution() = JourneyExecutionFixtures()

        fun giveMeOne(): JourneyExecutionFixtures {
            val id = Random.nextLong(1, 101)
            val journeyId = Random.nextLong(1, 101)
            val eventId = Random.nextLong(1, 101)
            val userId = Random.nextLong(1, 101)
            val triggerKey = "$journeyId:$eventId:$userId"
            return aJourneyExecution()
                .withId(id)
                .withJourneyId(journeyId)
                .withEventId(eventId)
                .withUserId(userId)
                .withTriggerKey(triggerKey)
        }

        fun aRunningExecution(): JourneyExecutionFixtures = giveMeOne().withStatus("RUNNING")

        fun aSuccessExecution(): JourneyExecutionFixtures =
            giveMeOne()
                .withStatus("SUCCESS")
                .withCompletedAt(LocalDateTime.now())

        fun aFailedExecution(): JourneyExecutionFixtures =
            giveMeOne()
                .withStatus("FAILED")
                .withLastError("Action dispatch failed")
                .withCompletedAt(LocalDateTime.now())
    }
}

package com.manage.crm.journey.domain

import java.time.LocalDateTime
import kotlin.random.Random

class JourneyFixtures private constructor() {
    private var id: Long? = null
    private var name: String = "default-journey-name"
    private var triggerType: String = "EVENT"
    private var triggerEventName: String? = "default-event"
    private var triggerSegmentId: Long? = null
    private var triggerSegmentEvent: String? = null
    private var triggerSegmentWatchFields: String? = null
    private var triggerSegmentCountThreshold: Long? = null
    private var active: Boolean = true
    private var lifecycleStatus: String = "ACTIVE"
    private var version: Int = 1
    private var createdAt: LocalDateTime? = null

    fun withId(id: Long?) = apply { this.id = id }

    fun withName(name: String) = apply { this.name = name }

    fun withTriggerType(triggerType: String) = apply { this.triggerType = triggerType }

    fun withTriggerEventName(triggerEventName: String?) = apply { this.triggerEventName = triggerEventName }

    fun withTriggerSegmentId(triggerSegmentId: Long?) = apply { this.triggerSegmentId = triggerSegmentId }

    fun withTriggerSegmentEvent(triggerSegmentEvent: String?) = apply { this.triggerSegmentEvent = triggerSegmentEvent }

    fun withTriggerSegmentWatchFields(json: String?) = apply { this.triggerSegmentWatchFields = json }

    fun withTriggerSegmentCountThreshold(threshold: Long?) = apply { this.triggerSegmentCountThreshold = threshold }

    fun withActive(active: Boolean) = apply { this.active = active }

    fun withLifecycleStatus(lifecycleStatus: String) = apply { this.lifecycleStatus = lifecycleStatus }

    fun withVersion(version: Int) = apply { this.version = version }

    fun withCreatedAt(createdAt: LocalDateTime?) = apply { this.createdAt = createdAt }

    fun build(): Journey =
        Journey(
            id = id,
            name = name,
            triggerType = triggerType,
            triggerEventName = triggerEventName,
            triggerSegmentId = triggerSegmentId,
            triggerSegmentEvent = triggerSegmentEvent,
            triggerSegmentWatchFields = triggerSegmentWatchFields,
            triggerSegmentCountThreshold = triggerSegmentCountThreshold,
            active = active,
            lifecycleStatus = lifecycleStatus,
            version = version,
            createdAt = createdAt,
        )

    companion object {
        fun aJourney() = JourneyFixtures()

        fun giveMeOne(): JourneyFixtures {
            val id = Random.nextLong(1, 101)
            val name = "journey_name_${Random.nextLong(1, 101)}"
            return aJourney()
                .withId(id)
                .withName(name)
        }

        fun anActiveJourney(): JourneyFixtures = giveMeOne().withActive(true).withLifecycleStatus("ACTIVE")

        fun anInactiveJourney(): JourneyFixtures = giveMeOne().withActive(false).withLifecycleStatus("DRAFT")

        fun anArchivedJourney(): JourneyFixtures = giveMeOne().withLifecycleStatus("ARCHIVED")

        fun anEventTriggeredJourney(eventName: String = "default-event"): JourneyFixtures =
            giveMeOne().withTriggerType("EVENT").withTriggerEventName(eventName)

        fun aSegmentTriggeredJourney(segmentId: Long = 1L): JourneyFixtures =
            giveMeOne()
                .withTriggerType("SEGMENT")
                .withTriggerEventName(null)
                .withTriggerSegmentId(segmentId)
                .withTriggerSegmentEvent("ENTER")

        fun aJourneyWithName(name: String): JourneyFixtures = giveMeOne().withName(name)
    }
}

package com.manage.crm.segment.domain

import java.time.LocalDateTime
import kotlin.random.Random

class SegmentFixtures private constructor() {
    private var id: Long? = null
    private var name: String = "default-segment-name"
    private var description: String? = null
    private var active: Boolean = true
    private var createdAt: LocalDateTime? = null

    fun withId(id: Long?) = apply { this.id = id }

    fun withName(name: String) = apply { this.name = name }

    fun withDescription(description: String?) = apply { this.description = description }

    fun withActive(active: Boolean) = apply { this.active = active }

    fun withCreatedAt(createdAt: LocalDateTime?) = apply { this.createdAt = createdAt }

    fun build(): Segment =
        Segment(
            id = id,
            name = name,
            description = description,
            active = active,
            createdAt = createdAt,
        )

    companion object {
        fun aSegment() = SegmentFixtures()

        fun giveMeOne(): SegmentFixtures {
            val id = Random.nextLong(1, 101)
            val name = "segment_name_${Random.nextLong(1, 101)}"
            val description = "Test segment description ${Random.nextInt(1, 101)}"
            val active = Random.nextBoolean()
            return aSegment()
                .withId(id)
                .withName(name)
                .withDescription(description)
                .withActive(active)
        }

        fun anActiveSegment(): SegmentFixtures = giveMeOne().withActive(true)

        fun anInactiveSegment(): SegmentFixtures = giveMeOne().withActive(false)

        fun aSegmentWithName(name: String): SegmentFixtures = giveMeOne().withName(name)
    }
}

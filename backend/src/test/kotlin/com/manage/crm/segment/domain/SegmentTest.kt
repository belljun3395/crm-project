package com.manage.crm.segment.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class SegmentTest : BehaviorSpec({
    given("Segment#new") {
        `when`("creating segment with explicit id") {
            val segment = SegmentFixtures.aSegment()
                .withId(10L)
                .withName("active-users")
                .withDescription("desc")
                .withActive(true)
                .build()

            then("initializes writable fields") {
                segment.id shouldBe 10L
                segment.name shouldBe "active-users"
                segment.description shouldBe "desc"
                segment.active shouldBe true
            }

            then("leaves persistence-managed createdAt unset") {
                segment.createdAt.shouldBeNull()
            }
        }

        `when`("creating segment without id") {
            val segment = SegmentFixtures.aSegment()
                .withName("new-segment")
                .withDescription(null)
                .withActive(false)
                .build()

            then("keeps id null before persistence") {
                segment.id.shouldBeNull()
                segment.active shouldBe false
            }
        }

        `when`("creating random segments") {
            val segment1 = SegmentFixtures.giveMeOne().build()
            val segment2 = SegmentFixtures.giveMeOne().build()

            then("produces different segments") {
                segment1.name shouldBe segment1.name
                segment2.name shouldBe segment2.name
                segment1.name != segment2.name
            }
        }

        `when`("creating active segment") {
            val segment = SegmentFixtures.anActiveSegment().build()

            then("segment is active") {
                segment.active shouldBe true
            }
        }

        `when`("creating inactive segment") {
            val segment = SegmentFixtures.anInactiveSegment().build()

            then("segment is inactive") {
                segment.active shouldBe false
            }
        }
    }
})

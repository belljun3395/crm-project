package com.manage.crm.segment.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class SegmentTest : BehaviorSpec({
    given("Segment#new") {
        `when`("creating segment with explicit id") {
            val segment = Segment.new(
                id = 10L,
                name = "active-users",
                description = "desc",
                active = true
            )

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
            val segment = Segment.new(
                name = "new-segment",
                description = null,
                active = false
            )

            then("keeps id null before persistence") {
                segment.id.shouldBeNull()
                segment.active shouldBe false
            }
        }
    }
})

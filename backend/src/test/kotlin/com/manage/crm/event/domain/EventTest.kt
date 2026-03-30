package com.manage.crm.event.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime

class EventTest : BehaviorSpec({
    given("Event#equals") {
        `when`("two events have the same field values") {
            val ts = LocalDateTime.of(2026, 1, 1, 0, 0)
            val props = PropertiesFixtures.giveMeOne().buildEvent()
            val a = Event.new(1L, "purchase", 10L, props, ts)
            val b = Event.new(1L, "purchase", 10L, props, ts)

            then("they are equal") {
                a shouldBe b
            }
        }

        `when`("two events differ by id") {
            val ts = LocalDateTime.of(2026, 1, 1, 0, 0)
            val props = PropertiesFixtures.giveMeOne().buildEvent()
            val a = Event.new(1L, "purchase", 10L, props, ts)
            val b = Event.new(2L, "purchase", 10L, props, ts)

            then("they are not equal") {
                a shouldNotBe b
            }
        }

        `when`("two events differ by name") {
            val ts = LocalDateTime.of(2026, 1, 1, 0, 0)
            val props = PropertiesFixtures.giveMeOne().buildEvent()
            val a = Event.new(1L, "view", 10L, props, ts)
            val b = Event.new(1L, "click", 10L, props, ts)

            then("they are not equal") {
                a shouldNotBe b
            }
        }

        `when`("two events differ by userId") {
            val ts = LocalDateTime.of(2026, 1, 1, 0, 0)
            val props = PropertiesFixtures.giveMeOne().buildEvent()
            val a = Event.new(1L, "view", 10L, props, ts)
            val b = Event.new(1L, "view", 99L, props, ts)

            then("they are not equal") {
                a shouldNotBe b
            }
        }
    }

    given("Event#hashCode") {
        `when`("two equal events") {
            val ts = LocalDateTime.of(2026, 1, 1, 0, 0)
            val props = PropertiesFixtures.giveMeOne().buildEvent()
            val a = Event.new(1L, "purchase", 10L, props, ts)
            val b = Event.new(1L, "purchase", 10L, props, ts)

            then("produce the same hash code") {
                a.hashCode() shouldBe b.hashCode()
            }
        }
    }
})

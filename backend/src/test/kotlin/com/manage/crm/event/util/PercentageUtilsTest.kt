package com.manage.crm.event.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PercentageUtilsTest :
    BehaviorSpec({
        given("toPercentage") {
            `when`("denominator is zero") {
                then("returns 0.0") {
                    toPercentage(10, 0) shouldBe 0.0
                }
            }

            `when`("denominator is negative") {
                then("returns 0.0") {
                    toPercentage(5, -1) shouldBe 0.0
                }
            }

            `when`("numerator is zero") {
                then("returns 0.0") {
                    toPercentage(0, 100) shouldBe 0.0
                }
            }

            `when`("numerator equals denominator") {
                then("returns 100.0") {
                    toPercentage(50, 50) shouldBe 100.0
                }
            }

            `when`("result is a clean percentage") {
                then("returns exact value") {
                    toPercentage(1, 4) shouldBe 25.0
                    toPercentage(3, 4) shouldBe 75.0
                }
            }

            `when`("result requires rounding to 2 decimal places") {
                then("returns rounded value") {
                    toPercentage(1, 3) shouldBe 33.33
                    toPercentage(2, 3) shouldBe 66.67
                }
            }

            `when`("numerator is greater than denominator") {
                then("returns value greater than 100") {
                    toPercentage(3, 2) shouldBe 150.0
                }
            }
        }
    })

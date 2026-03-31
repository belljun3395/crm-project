package com.manage.crm.segment.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.manage.crm.segment.util.SegmentConditionValidator
import com.manage.crm.segment.exception.InvalidSegmentConditionException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain

class SegmentConditionValidatorTest : BehaviorSpec({
    val objectMapper = jacksonObjectMapper()

    given("SegmentConditionValidator") {
        `when`("field is blank") {
            then("throws field required") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "  ",
                        operator = "EQ",
                        valueType = "STRING",
                        value = objectMapper.readTree("\"v\"")
                    )
                }
                ex.message shouldContain "field is required"
            }
        }

        `when`("field is not in supported list") {
            then("throws unsupported field") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.unknown",
                        operator = "EQ",
                        valueType = "STRING",
                        value = objectMapper.readTree("\"v\"")
                    )
                }
                ex.message shouldContain "Unsupported field"
            }
        }

        `when`("valueType does not match field requirement") {
            then("throws type mismatch") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.id",
                        operator = "EQ",
                        valueType = "STRING",
                        value = objectMapper.readTree("\"100\"")
                    )
                }
                ex.message shouldContain "requires valueType"
            }
        }

        `when`("valueType is unknown") {
            then("throws unsupported valueType") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.email",
                        operator = "EQ",
                        valueType = "TEXT",
                        value = objectMapper.readTree("\"a@b.com\"")
                    )
                }
                ex.message shouldContain "Unsupported valueType"
            }
        }

        `when`("operator is not allowed for the valueType") {
            then("throws operator not allowed") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.email",
                        operator = "GT",
                        valueType = "STRING",
                        value = objectMapper.readTree("\"a@b.com\"")
                    )
                }
                ex.message shouldContain "not allowed for valueType"
            }
        }

        `when`("operator is unknown") {
            then("throws unsupported operator") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.email",
                        operator = "MATCHES",
                        valueType = "STRING",
                        value = objectMapper.readTree("\"a@b.com\"")
                    )
                }
                ex.message shouldContain "Unsupported operator"
            }
        }

        `when`("value is null node") {
            then("throws value required") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.email",
                        operator = "EQ",
                        valueType = "STRING",
                        value = objectMapper.readTree("null")
                    )
                }
                ex.message shouldContain "value is required"
            }
        }

        `when`("value is missing node") {
            then("throws value required") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.email",
                        operator = "EQ",
                        valueType = "STRING",
                        value = objectMapper.readTree("{}").path("missing")
                    )
                }
                ex.message shouldContain "value is required"
            }
        }

        `when`("IN operator receives non-array value") {
            then("throws array required") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.email",
                        operator = "IN",
                        valueType = "STRING",
                        value = objectMapper.readTree("\"single@b.com\"")
                    )
                }
                ex.message shouldContain "non-empty array"
            }
        }

        `when`("IN operator receives empty array") {
            then("throws non-empty array required") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.email",
                        operator = "IN",
                        valueType = "STRING",
                        value = objectMapper.readTree("[]")
                    )
                }
                ex.message shouldContain "non-empty array"
            }
        }

        `when`("BETWEEN operator receives array with one item") {
            then("throws exactly two items required") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.id",
                        operator = "BETWEEN",
                        valueType = "NUMBER",
                        value = objectMapper.readTree("[1]")
                    )
                }
                ex.message shouldContain "exactly two items"
            }
        }

        `when`("non-array operator receives array value") {
            then("throws array not allowed") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.email",
                        operator = "EQ",
                        valueType = "STRING",
                        value = objectMapper.readTree("[\"a@b.com\"]")
                    )
                }
                ex.message shouldContain "does not allow array"
            }
        }

        `when`("STRING field with non-string scalar value") {
            then("throws string value required") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.email",
                        operator = "EQ",
                        valueType = "STRING",
                        value = objectMapper.readTree("123")
                    )
                }
                ex.message shouldContain "STRING value must be a string"
            }
        }

        `when`("STRING IN with non-string array items") {
            then("throws string items required") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.email",
                        operator = "IN",
                        valueType = "STRING",
                        value = objectMapper.readTree("[1, 2]")
                    )
                }
                ex.message shouldContain "STRING IN value must contain only string items"
            }
        }

        `when`("NUMBER field with non-numeric value") {
            then("throws numeric value required") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.id",
                        operator = "EQ",
                        valueType = "NUMBER",
                        value = objectMapper.readTree("\"not-a-number\"")
                    )
                }
                ex.message shouldContain "NUMBER value must be numeric"
            }
        }

        `when`("DATETIME field with non-ISO string") {
            then("throws ISO-8601 format required") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.createdAt",
                        operator = "EQ",
                        valueType = "DATETIME",
                        value = objectMapper.readTree("\"2024-13-99\"")
                    )
                }
                ex.message shouldContain "ISO-8601"
            }
        }

        `when`("DATETIME BETWEEN with non-ISO string items") {
            then("throws ISO-8601 items required") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.createdAt",
                        operator = "BETWEEN",
                        valueType = "DATETIME",
                        value = objectMapper.readTree("[\"invalid\", \"also-invalid\"]")
                    )
                }
                ex.message shouldContain "ISO-8601"
            }
        }

        `when`("NUMBER field with boolean value") {
            then("throws numeric value required") {
                val ex = shouldThrow<InvalidSegmentConditionException> {
                    SegmentConditionValidator.validate(
                        field = "user.id",
                        operator = "EQ",
                        valueType = "NUMBER",
                        value = objectMapper.readTree("true")
                    )
                }
                ex.message shouldContain "NUMBER value must be numeric"
            }
        }

        `when`("valid STRING EQ condition") {
            then("passes without exception") {
                SegmentConditionValidator.validate(
                    field = "user.email",
                    operator = "EQ",
                    valueType = "STRING",
                    value = objectMapper.readTree("\"valid@example.com\"")
                )
            }
        }

        `when`("valid STRING IN condition") {
            then("passes without exception") {
                SegmentConditionValidator.validate(
                    field = "user.email",
                    operator = "IN",
                    valueType = "STRING",
                    value = objectMapper.readTree("[\"a@example.com\", \"b@example.com\"]")
                )
            }
        }

        `when`("valid NUMBER BETWEEN condition") {
            then("passes without exception") {
                SegmentConditionValidator.validate(
                    field = "user.id",
                    operator = "BETWEEN",
                    valueType = "NUMBER",
                    value = objectMapper.readTree("[1, 100]")
                )
            }
        }

        `when`("valid lowercase operator/valueType is provided") {
            then("passes without exception") {
                SegmentConditionValidator.validate(
                    field = "user.email",
                    operator = "eq",
                    valueType = "string",
                    value = objectMapper.readTree("\"valid@example.com\"")
                )
            }
        }

        `when`("valid DATETIME GT condition") {
            then("passes without exception") {
                SegmentConditionValidator.validate(
                    field = "user.createdAt",
                    operator = "GT",
                    valueType = "DATETIME",
                    value = objectMapper.readTree("\"2024-01-01T00:00:00\"")
                )
            }
        }
    }
})

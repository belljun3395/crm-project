package com.manage.crm.segment.controller.request

import com.fasterxml.jackson.databind.node.TextNode
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SegmentRequestValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `post request rejects multiline name`() {
        val request = PostSegmentRequest(
            name = "New\nSegment",
            conditions = listOf(validCondition())
        )

        val violations = validator.validate(request)

        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `post request accepts single-line non-blank name`() {
        val request = PostSegmentRequest(
            name = "New Segment",
            conditions = listOf(validCondition())
        )

        val violations = validator.validate(request)

        assertFalse(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `put request rejects blank name`() {
        val request = PutSegmentRequest(name = "  ")

        val violations = validator.validate(request)

        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    @Test
    fun `put request rejects multiline name`() {
        val request = PutSegmentRequest(name = "A\nB")

        val violations = validator.validate(request)

        assertTrue(violations.any { it.propertyPath.toString() == "name" })
    }

    private fun validCondition(): SegmentConditionRequest {
        return SegmentConditionRequest(
            field = "email",
            operator = "EQ",
            valueType = "STRING",
            value = TextNode.valueOf("vip@example.com")
        )
    }
}

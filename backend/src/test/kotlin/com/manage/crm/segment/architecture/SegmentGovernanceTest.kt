package com.manage.crm.segment.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/**
 * Enforces UC contract governance rules for the segment module.
 *
 * Rules:
 * 1. Every UseCase in segment.application must have a KDoc comment.
 * 2. Every UseCase KDoc must contain a UC code (UC-XXX-NNN).
 * 3. Every UseCase KDoc must have an Input: section.
 * 4. Every UseCase KDoc must have a Success: section.
 * 5. Every UseCase test file must reference the same UC code in a given() block.
 */
class SegmentGovernanceTest {

    @Test
    fun `segment use case classes have kdoc`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .filter { it.resideInPackage("..segment.application..") }
            .assertTrue(additionalMessage = "Add a KDoc comment to the UseCase class") {
                it.hasKDoc
            }
    }

    @Test
    fun `segment use case kdoc contains UC code`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .filter { it.resideInPackage("..segment.application..") }
            .assertTrue(additionalMessage = "KDoc must contain a UC code matching UC-XXX-NNN") {
                it.kDoc?.text?.contains(Regex("UC-[A-Z]+-\\d+")) == true
            }
    }

    @Test
    fun `segment use case kdoc contains Input section`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .filter { it.resideInPackage("..segment.application..") }
            .assertTrue(additionalMessage = "KDoc must contain an 'Input:' section describing scenarios") {
                it.kDoc?.text?.contains("Input:") == true
            }
    }

    @Test
    fun `segment use case kdoc contains Success section`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .filter { it.resideInPackage("..segment.application..") }
            .assertTrue(additionalMessage = "KDoc must contain a 'Success:' section") {
                it.kDoc?.text?.contains("Success:") == true
            }
    }

    @Test
    fun `segment use case test files reference UC code in given block`() {
        val useCaseUcCodes = Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .filter { it.resideInPackage("..segment.application..") }
            .mapNotNull { clazz ->
                val ucCode = clazz.kDoc?.text
                    ?.let { Regex("UC-[A-Z]+-\\d+").find(it)?.value }
                if (ucCode != null) clazz.name to ucCode else null
            }
            .toMap()

        val testScope = Konsist.scopeFromTest()

        useCaseUcCodes.forEach { (useCaseName, ucCode) ->
            val testFile = testScope.files.firstOrNull { it.name == "${useCaseName}Test" }
            checkNotNull(testFile) {
                "[FAIL] No test file found for $useCaseName (expected ${useCaseName}Test.kt)"
            }
            check(testFile.text.contains(ucCode)) {
                "[FAIL] Test file ${testFile.name}.kt does not reference UC code '$ucCode' — add it to the given() block"
            }
        }
    }
}

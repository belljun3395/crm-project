package com.manage.crm.event.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/**
 * Enforces UC contract governance rules for the event module.
 *
 * Rules:
 * 1. Every UseCase in event.application must have a KDoc comment.
 * 2. Every UseCase KDoc must contain a UC code (UC-XXX-NNN).
 * 3. Every UseCase KDoc must have an Input: section.
 * 4. Every UseCase KDoc must have a Success: section.
 * 5. Every UseCase test file must reference the same UC code in a given() block.
 */
class EventGovernanceTest {

    @Test
    fun `event use case classes have kdoc`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .filter { it.resideInPackage("..event.application..") }
            .assertTrue(additionalMessage = "Add a KDoc comment to the UseCase class") {
                it.hasKDoc
            }
    }

    @Test
    fun `event use case kdoc contains UC code`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .filter { it.resideInPackage("..event.application..") }
            .assertTrue(additionalMessage = "KDoc must contain a UC code matching UC-XXX-NNN") {
                it.kDoc?.text?.contains(Regex("UC-[A-Z]+-\\d+")) == true
            }
    }

    @Test
    fun `event use case kdoc contains Input section`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .filter { it.resideInPackage("..event.application..") }
            .assertTrue(additionalMessage = "KDoc must contain an 'Input:' section describing scenarios") {
                it.kDoc?.text?.contains("Input:") == true
            }
    }

    @Test
    fun `event use case kdoc contains Success section`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .filter { it.resideInPackage("..event.application..") }
            .assertTrue(additionalMessage = "KDoc must contain a 'Success:' section") {
                it.kDoc?.text?.contains("Success:") == true
            }
    }

    @Test
    fun `event use case test files reference UC code in given block`() {
        val useCaseUcCodes = Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .filter { it.resideInPackage("..event.application..") }
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

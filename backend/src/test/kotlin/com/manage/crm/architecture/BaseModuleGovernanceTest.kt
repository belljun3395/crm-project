package com.manage.crm.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

abstract class BaseModuleGovernanceTest {
    protected abstract val spec: ModuleRuleSpec

    @Test
    fun `use case classes have kdoc`() {
        useCaseClasses().assertTrue(additionalMessage = "Add a KDoc comment to the UseCase class") {
            it.hasKDoc
        }
    }

    @Test
    fun `use case kdoc contains UC code`() {
        useCaseClasses().assertTrue(additionalMessage = "KDoc must contain a UC code matching UC-XXX-NNN") {
            it.kDoc?.text?.contains(spec.ucCodeRegex) == true
        }
    }

    @Test
    fun `use case kdoc contains Input section`() {
        useCaseClasses().assertTrue(additionalMessage = "KDoc must contain an 'Input:' section describing scenarios") {
            it.kDoc?.text?.contains("Input:") == true
        }
    }

    @Test
    fun `use case kdoc contains Success section`() {
        useCaseClasses().assertTrue(additionalMessage = "KDoc must contain a 'Success:' section") {
            it.kDoc?.text?.contains("Success:") == true
        }
    }

    @Test
    fun `use case test files reference UC code in given block`() {
        val useCaseUcCodes = loadUseCaseUcCodes().associate { it.useCaseName to it.code }
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

    @Test
    fun `use case UC codes are unique`() {
        val duplicatedCodes = loadUseCaseUcCodes()
            .groupBy { it.code }
            .filterValues { it.size > 1 }

        check(duplicatedCodes.isEmpty()) {
            duplicatedCodes.entries.joinToString(
                prefix = "[FAIL] Duplicate UC codes found: ",
                separator = ", "
            ) { (code, entries) ->
                "$code -> ${entries.joinToString("/") { it.useCaseName }}"
            }
        }
    }

    @Test
    fun `use case UC sequence is contiguous per domain`() {
        val ucCodesByDomain = loadUseCaseUcCodes().groupBy { it.domain }

        ucCodesByDomain.forEach { (domain, ucCodes) ->
            val numbers = ucCodes
                .map { it.sequence }
                .distinct()
                .sorted()

            if (numbers.isEmpty()) {
                return@forEach
            }

            check(numbers.first() == 1) {
                "[FAIL] UC-$domain numbering must start from 001 but starts from ${numbers.first().toThreeDigits()}"
            }

            val expected = (1..numbers.last()).toList()
            check(numbers == expected) {
                val missing = expected
                    .toSet()
                    .minus(numbers.toSet())
                    .sorted()
                    .joinToString(", ") { it.toThreeDigits() }
                "[FAIL] UC-$domain numbering has gaps. Missing: $missing"
            }
        }
    }

    private fun useCaseClasses() = Konsist
        .scopeFromProduction()
        .classes()
        .withNameEndingWith("UseCase")
        .filter { it.resideInPackage(spec.applicationPackagePattern) }

    private fun loadUseCaseUcCodes(): List<UseCaseUcCode> {
        return useCaseClasses().mapNotNull { clazz ->
            val match = clazz.kDoc?.text?.let { spec.ucCodeRegex.find(it) } ?: return@mapNotNull null
            check(match.groupValues.size >= 3) {
                "UC regex must expose domain and sequence groups. regex=${spec.ucCodeRegex.pattern}"
            }

            val sequence = match.groupValues[2].toIntOrNull()
            checkNotNull(sequence) {
                "UC code sequence should be numeric. code=${match.value}"
            }

            UseCaseUcCode(
                useCaseName = clazz.name,
                domain = match.groupValues[1],
                sequence = sequence,
                code = match.value
            )
        }
    }
}

private data class UseCaseUcCode(
    val useCaseName: String,
    val domain: String,
    val sequence: Int,
    val code: String
)

private fun Int.toThreeDigits(): String = toString().padStart(3, '0')

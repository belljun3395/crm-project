package com.manage.crm.email.domain.support

import com.manage.crm.email.domain.vo.DELIMITER
import com.manage.crm.email.domain.vo.LEGACY_TYPE_DELIMITER
import com.manage.crm.email.domain.vo.SOURCE_DELIMITER
import com.manage.crm.email.domain.vo.VariableSource

/**
 * Parses variable declarations in both new and legacy formats.
 *
 * - New format:    `source.key` or `source.key:default`   (e.g., `user.email`, `campaign.eventCount:0`)
 * - Legacy format: `source_key` or `source_key:default`   (e.g., `user_email`, `campaign_eventCount:0`)
 *
 * Both formats normalize to the same internal representation: (VariableSource, key, defaultValue?).
 */
object VariableParser {

    fun parse(value: String): Triple<VariableSource, String, String?> {
        val namePart = value.substringBefore(DELIMITER)
        val defaultValue = if (value.contains(DELIMITER)) value.substringAfter(DELIMITER) else null

        return if (isNewFormat(namePart)) {
            parseNewFormat(namePart, defaultValue)
        } else {
            parseLegacyFormat(namePart, defaultValue)
        }
    }

    /**
     * Returns true only when the part before the first dot is a valid [VariableSource] value.
     * This guards against legacy keys that happen to contain dots (e.g., `user_profile.name`),
     * which would otherwise be mis-classified as new-format variables.
     */
    private fun isNewFormat(namePart: String): Boolean {
        if (!namePart.contains(SOURCE_DELIMITER)) return false
        val potentialSource = namePart.substringBefore(SOURCE_DELIMITER)
        return VariableSource.entries.any { it.value == potentialSource }
    }

    private fun parseNewFormat(namePart: String, defaultValue: String?): Triple<VariableSource, String, String?> {
        val source = VariableSource.fromValue(namePart.substringBefore(SOURCE_DELIMITER))
        val key = namePart.substringAfter(SOURCE_DELIMITER)
        return Triple(source, key, defaultValue)
    }

    private fun parseLegacyFormat(namePart: String, defaultValue: String?): Triple<VariableSource, String, String?> {
        val source = VariableSource.fromValue(namePart.substringBefore(LEGACY_TYPE_DELIMITER))
        val key = namePart.substringAfter(LEGACY_TYPE_DELIMITER)
        return Triple(source, key, defaultValue)
    }
}

package com.manage.crm.email.domain.support

import com.manage.crm.email.domain.vo.CampaignVariable
import com.manage.crm.email.domain.vo.UserVariable
import com.manage.crm.email.domain.vo.VariableSource
import com.manage.crm.email.domain.vo.Variables

/**
 * Converts a list of variable declaration strings to a [Variables] instance.
 *
 * Accepts both the new standard format (`user.email`) and the legacy format (`user_email`).
 * Parsing is delegated to [VariableParser], which normalizes both forms to the same
 * internal representation.
 */
fun List<String>.stringListToVariables(): Variables {
    return this.map {
        val (source, key, defaultValue) = VariableParser.parse(it)
        return@map when (source) {
            VariableSource.USER -> UserVariable(key, defaultValue)
            VariableSource.CAMPAIGN -> CampaignVariable(key, defaultValue)
        }
    }.let { Variables(it) }
}

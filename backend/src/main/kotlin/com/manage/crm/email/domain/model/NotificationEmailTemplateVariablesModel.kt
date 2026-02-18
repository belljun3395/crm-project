package com.manage.crm.email.domain.model

import com.manage.crm.email.domain.vo.CampaignVariable
import com.manage.crm.email.domain.vo.UserVariable
import com.manage.crm.email.domain.vo.VariableSource
import com.manage.crm.email.domain.vo.Variables

data class NotificationEmailTemplateVariablesModel(
    val subject: String,
    val body: String,
    val variables: Variables
) {
    fun isNoVariables(): Boolean = variables.isEmpty()

    fun getCampaignVariables(): List<CampaignVariable> =
        variables.filterBySource(VariableSource.CAMPAIGN).filterIsInstance<CampaignVariable>()

    fun getUserVariables(): List<UserVariable> =
        variables.filterBySource(VariableSource.USER).filterIsInstance<UserVariable>()
}

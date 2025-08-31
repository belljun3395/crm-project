package com.manage.crm.email.domain.model

import com.manage.crm.email.domain.vo.CAMPAIGN_TYPE
import com.manage.crm.email.domain.vo.CampaignVariable
import com.manage.crm.email.domain.vo.USER_TYPE
import com.manage.crm.email.domain.vo.UserVariable
import com.manage.crm.email.domain.vo.Variables

data class NotificationEmailTemplateVariablesModel(
    val subject: String,
    val body: String,
    val variables: Variables
) {
    fun isNoVariables(): Boolean {
        return variables.isEmpty()
    }

    fun getCampaignVariables(): List<CampaignVariable> {
        return variables.filterByType(CAMPAIGN_TYPE).map { it as CampaignVariable }
    }

    fun getUserVariables(): List<UserVariable> {
        return variables.filterByType(USER_TYPE).map { it as UserVariable }
    }
}

package com.manage.crm.email.domain.model

import com.manage.crm.email.domain.vo.Variables

data class NotificationEmailTemplatePropertiesModel(
    val subject: String,
    val body: String,
    val variables: Variables
) {
    fun isNoVariables(): Boolean {
        return variables.isEmpty()
    }
}

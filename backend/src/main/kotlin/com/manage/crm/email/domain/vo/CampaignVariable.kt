package com.manage.crm.email.domain.vo

class CampaignVariable(
    key: String,
    defaultValue: String? = null
) : Variable(CAMPAIGN_TYPE, key, defaultValue)

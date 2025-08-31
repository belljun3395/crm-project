package com.manage.crm.email.domain.vo

class UserVariable(
    key: String,
    defaultValue: String? = null
) : Variable(USER_TYPE, key, defaultValue)

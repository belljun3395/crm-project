package com.manage.crm.email.domain.vo

data class Variables(
    val value: List<String> = emptyList()
) {
    constructor(vararg value: String) : this(value.toList())
}

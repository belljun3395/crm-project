package com.manage.crm.email.domain.vo

data class Email(
    val value: String
) {
    companion object {
        private const val EMAIL_FORMAT = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    }
    init {
        require(value.isNotBlank()) { "Email cannot be blank" }
        require(value.matches(Regex(EMAIL_FORMAT))) { "Email format is invalid" }
    }
}

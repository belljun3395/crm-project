package com.manage.crm.email.domain.vo

enum class ScheduleType {
    AWS, APP;

    companion object {
        fun contains(value: String) = entries.any { it.name.equals(value, true) }
    }
}

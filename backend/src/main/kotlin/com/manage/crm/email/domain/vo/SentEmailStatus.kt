package com.manage.crm.email.domain.vo

enum class SentEmailStatus(val code: Byte) {
    OPEN(0),
    DELIVERY(1),
    CLICK(2),
    SEND(3),
    DELIVERYDELAY(4)

    ;

    companion object {
        fun contains(value: String) = entries.any { it.name.equals(value, true) }
        fun fromCode(code: Byte): SentEmailStatus? = entries.find { it.code == code }
    }
}

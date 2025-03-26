package com.manage.crm.infrastructure.mail

fun String.withTitle(title: String): String {
    return "$title <$this>"
}

class EmailExtension

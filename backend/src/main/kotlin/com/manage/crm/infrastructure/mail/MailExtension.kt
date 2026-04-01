package com.manage.crm.infrastructure.mail

fun String.withTitle(title: String): String = "$title <$this>"

class EmailExtension

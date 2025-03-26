package com.manage.crm.infrastructure.mail

interface SendMailArgs<C, P> {
    val to: String
    val subject: String
    val template: String
    val content: C
    val properties: P
}

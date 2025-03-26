package com.manage.crm.infrastructure.mail

import com.manage.crm.infrastructure.mail.provider.MailSendProvider
import org.springframework.boot.autoconfigure.mail.MailProperties

abstract class MailSender<T : SendMailArgs<*, *>>(
    private val mailProperties: MailProperties,
    private val defaultMailSendProvider: MailSendProvider
) {
    fun send(args: T, mailSendProvider: MailSendProvider? = null): String {
        val from = mailProperties.username
        val to = args.to
        val subject = args.subject
        val message = getHtml(args)

        return mailSendProvider?.sendEmail(from, to, subject, message)
            ?: run {
                defaultMailSendProvider.sendEmail(from, to, subject, message)
            }
    }

    abstract fun getHtml(args: T): String
}

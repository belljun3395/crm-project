package com.manage.crm.email.application.dto

import com.manage.crm.email.domain.vo.EmailProviderType
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.infrastructure.mail.SendMailArgs

abstract class Content {
    abstract fun getKeys(): List<String>
    abstract fun getValue(key: String): String
    fun merge(content: Content): Content {
        val originKeys = this.getKeys()
        val newKeys = content.getKeys()
        if (originKeys.isEmpty() && newKeys.isEmpty()) {
            return NonContent()
        }

        val combinedKeys = (originKeys + newKeys).distinct()
        val combinedContent = mutableMapOf<String, String>()
        combinedKeys.forEach { key ->
            // newKeys priority over originKeys because this method is used to merge new content into existing content
            val value = when {
                newKeys.contains(key) -> content.getValue(key)
                originKeys.contains(key) -> this.getValue(key)
                else -> null
            }
            if (value != null) {
                combinedContent[key] = value
            }
        }
        return VariablesContent(combinedContent)
    }
}
class NonContent : Content() {
    override fun getKeys(): List<String> = emptyList()
    override fun getValue(key: String): String = ""
}

class VariablesContent(val variables: Map<String, String>) : Content() {
    override fun getKeys(): List<String> = variables.keys.toList()
    override fun getValue(key: String): String = variables[key] ?: ""
}

data class SendEmailArgs(
    override val to: String,
    override val subject: String,
    override val template: String,
    override val content: Content,
    override val properties: String = ""
) : SendMailArgs<Content, String>

data class SendEmailInDto(
    val to: String,
    val subject: String,
    val template: String,
    val content: Content,
    val properties: String = "",
    val emailBody: String,
    val destination: String,
    val eventType: SentEmailStatus
) {
    val emailArgs = SendEmailArgs(to, subject, template, content, properties)
}

data class SendEmailOutDto(
    val userId: Long,
    val emailBody: String,
    val messageId: String,
    val destination: String,
    val provider: EmailProviderType
)

class MailServiceDto

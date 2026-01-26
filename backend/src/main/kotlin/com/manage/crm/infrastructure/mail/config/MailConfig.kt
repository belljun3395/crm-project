package com.manage.crm.infrastructure.mail.config

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.mail.MailProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.*

@Configuration
class MailConfig {
    companion object {
        const val MAIL_PROPERTIES = "mailProperties"
        const val MAIL_SENDER = "javaMailSender"
        const val AWS_EMAIL_SENDER = "awsEmailProvider"

        const val MAIL_SMTP_AUTH_KEY = "mail.smtp.auth"
        const val MAIL_SMTP_DEBUG_KEY = "mail.smtp.debug"
        const val MAIL_SMTP_STARTTLS_ENABLE_KEY = "mail.smtp.starttls.enable"
        const val MAIL_SMTP_ENABLE_SSL_ENABLE_KEY = "mail.smtp.EnableSSL.enable"
    }

    @Value("\${spring.mail.protocol}")
    private lateinit var protocol: String

    @Value("\${spring.mail.host}")
    private lateinit var host: String

    @Value("\${spring.mail.port}")
    private lateinit var port: String

    @Value("\${spring.mail.username}")
    private lateinit var username: String

    @Value("\${spring.mail.password}")
    private lateinit var password: String

    @Value("\${spring.mail.properties.mail.smtp.auth}")
    private lateinit var auth: String

    @Value("\${spring.mail.properties.mail.smtp.debug}")
    private lateinit var debug: String

    @Value("\${spring.mail.properties.mail.smtp.starttls.enable}")
    private lateinit var starttls: String

    @Value("\${spring.mail.properties.mail.smtp.EnableSSL.enable}")
    private lateinit var enableSSL: String

    @Bean(name = [MAIL_PROPERTIES])
    fun mailProperties(): MailProperties {
        val mailProperties = MailProperties()
        mailProperties.protocol = protocol
        mailProperties.host = host
        mailProperties.port = port.toInt()
        mailProperties.username = username
        mailProperties.password = password
        mailProperties.properties[MAIL_SMTP_AUTH_KEY] = auth
        mailProperties.properties[MAIL_SMTP_DEBUG_KEY] = debug
        mailProperties.properties[MAIL_SMTP_STARTTLS_ENABLE_KEY] = starttls
        mailProperties.properties[MAIL_SMTP_ENABLE_SSL_ENABLE_KEY] = enableSSL
        return mailProperties
    }

    // ----------------- Java Mail Config -----------------
    @Bean(name = [MAIL_SENDER])
    @ConditionalOnProperty(name = ["mail.provider"], havingValue = "javamail")
    fun javaMailSender(): JavaMailSender {
        val javaMailSender = JavaMailSenderImpl()
        val mailProperties = mailProperties()

        javaMailSender.protocol = mailProperties.protocol
        javaMailSender.host = mailProperties.host
        javaMailSender.port = mailProperties.port
        javaMailSender.username = mailProperties.username
        javaMailSender.password = mailProperties.password

        val props = Properties()
        props[MAIL_SMTP_AUTH_KEY] = mailProperties.properties[MAIL_SMTP_AUTH_KEY]
        props[MAIL_SMTP_DEBUG_KEY] = mailProperties.properties[MAIL_SMTP_DEBUG_KEY]
        props[MAIL_SMTP_STARTTLS_ENABLE_KEY] =
            mailProperties.properties[MAIL_SMTP_STARTTLS_ENABLE_KEY]
        props[MAIL_SMTP_ENABLE_SSL_ENABLE_KEY] = mailProperties.properties[MAIL_SMTP_ENABLE_SSL_ENABLE_KEY]

        javaMailSender.javaMailProperties = props
        return javaMailSender
    }

    // ----------------- AWS Config -----------------
    @Value("\${spring.aws.region}")
    val region: String? = null

    @Value("\${spring.aws.endpoint-url:#{null}}")
    val endpointUrl: String? = null

    @Bean(name = [AWS_EMAIL_SENDER])
    @ConditionalOnBean(AWSCredentials::class)
    @ConditionalOnProperty(name = ["mail.provider"], havingValue = "aws")
    fun awsEmailSender(awsCredentials: AWSCredentials): AmazonSimpleEmailService {
        val awsStaticCredentialsProvider = AWSStaticCredentialsProvider(awsCredentials)
        val clientBuilder = AmazonSimpleEmailServiceClientBuilder
            .standard()
            .withCredentials(awsStaticCredentialsProvider)

        // Configure endpoint URL for LocalStack or use region for AWS
        endpointUrl?.let {
            clientBuilder.withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(endpointUrl, region))
        } ?: clientBuilder.withRegion(region)

        return clientBuilder.build()
    }
}

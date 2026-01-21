package com.manage.crm.config

import com.manage.crm.email.domain.repository.converter.EmailTemplateVersionReadingConverter
import com.manage.crm.email.domain.repository.converter.EmailTemplateVersionWritingConverter
import com.manage.crm.email.domain.repository.converter.EventIdReadingConverter
import com.manage.crm.email.domain.repository.converter.EventIdWritingConverter
import com.manage.crm.email.domain.repository.converter.UserEmailReadingConverter
import com.manage.crm.email.domain.repository.converter.UserEmailWritingConverter
import com.manage.crm.email.domain.repository.converter.VariablesReadingConverter
import com.manage.crm.email.domain.repository.converter.VariablesWritingConverter
import com.manage.crm.event.domain.repository.converter.PropertiesReadingConverter
import com.manage.crm.event.domain.repository.converter.PropertiesWritingConverter
import com.manage.crm.user.domain.repository.converter.UserAttributeReadingConverter
import com.manage.crm.user.domain.repository.converter.UserAttributeWritingConverter
import com.manage.crm.webhook.domain.repository.WebhookEventsReadingConverter
import com.manage.crm.webhook.domain.repository.WebhookEventsWritingConverter
import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.convert.CustomConversions
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.DialectResolver
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.ReactiveTransactionManager

@Configuration
@EnableR2dbcAuditing
class R2dbcConfig {
    companion object {
        const val TRANSACTION_MANAGER = "transactionManager"
        const val R2DBC_CUSTOM_CONVERSIONS = "r2dbcCustomConversions"
    }

    @Bean(name = [TRANSACTION_MANAGER])
    fun transactionManager(connectionFactory: ConnectionFactory): ReactiveTransactionManager {
        return R2dbcTransactionManager(connectionFactory)
    }

    @Bean(name = [R2DBC_CUSTOM_CONVERSIONS])
    fun r2dbcCustomConversions(databaseClient: DatabaseClient): R2dbcCustomConversions {
        val dialect = DialectResolver.getDialect(databaseClient.connectionFactory)
        val converters = ArrayList(dialect.converters)
        converters.addAll(R2dbcCustomConversions.STORE_CONVERTERS)

        return R2dbcCustomConversions(
            CustomConversions.StoreConversions.of(dialect.simpleTypeHolder, converters),
            listOf(
                UserAttributeWritingConverter(),
                UserAttributeReadingConverter(),
                VariablesWritingConverter(),
                VariablesReadingConverter(),
                EventIdWritingConverter(),
                EventIdReadingConverter(),
                PropertiesWritingConverter(),
                PropertiesReadingConverter(),
                WebhookEventsWritingConverter(),
                WebhookEventsReadingConverter(),
                EmailTemplateVersionReadingConverter(),
                EmailTemplateVersionWritingConverter(),
                UserEmailReadingConverter(),
                UserEmailWritingConverter()
            )
        )
    }
}

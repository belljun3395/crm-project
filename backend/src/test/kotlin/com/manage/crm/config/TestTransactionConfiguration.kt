package com.manage.crm.config

import com.amazonaws.auth.AWSCredentials
import com.manage.crm.infrastructure.AwsConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import javax.sql.DataSource

@TestConfiguration
class TestTransactionConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    fun dataSource(): DataSource = DataSourceBuilder.create().type(HikariDataSource::class.java).build()

    @Bean
    fun transactionalManager(dataSource: DataSource): PlatformTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }

    @Bean
    fun transactionalTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate {
        return TransactionTemplate(transactionManager)
    }

    @Bean(name = [AwsConfig.AWS_CREDENTIAL_PROVIDER])
    @ConditionalOnMissingBean(AWSCredentials::class)
    fun testAwsCredentials(): AWSCredentials = object : AWSCredentials {
        override fun getAWSAccessKeyId(): String = "test-access-key"
        override fun getAWSSecretKey(): String = "test-secret-key"
    }
}

package com.manage.crm.webhook.domain.repository.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@Configuration
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
@EnableR2dbcRepositories(basePackages = ["com.manage.crm.webhook.domain.repository"])
class EnableWebhookRepositoriesConfig

package com.manage.crm.audit.domain.repository.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

/**
 * Enables R2DBC repositories for the audit bounded context.
 */
@Configuration
@EnableR2dbcRepositories(basePackages = ["com.manage.crm.audit.domain.repository"])
class EnableAuditRepositoriesConfig

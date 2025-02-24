package com.manage.crm.email.domain.repository.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@Configuration
@EnableR2dbcRepositories(basePackages = ["com.manage.crm.email.domain.repository"])
class EnableEmailRepositoriesConfig

package com.manage.crm.segment.domain.repository.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@Configuration
@EnableR2dbcRepositories(basePackages = ["com.manage.crm.segment.domain.repository"])
class EnableSegmentRepositoriesConfig

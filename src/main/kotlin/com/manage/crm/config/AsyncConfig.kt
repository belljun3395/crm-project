package com.manage.crm.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 10
            maxPoolSize = 20
            setThreadNamePrefix("default-async")
            initialize()
        }
    }
}

package com.manage.crm.email

import com.manage.crm.config.RedisKafkaContainerInitializer
import com.manage.crm.config.TestTransactionConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@TestPropertySource(properties = ["message.provider=kafka", "scheduler.provider=redis-kafka", "mail.provider=javamail"])
@ActiveProfiles(value = ["test", "new"])
@EnableAutoConfiguration(
    exclude = [FlywayAutoConfiguration::class]
)
@ContextConfiguration(initializers = [RedisKafkaContainerInitializer::class])
@ApplicationModuleTest(
    module = "email",
    mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES,
    classes = [TestTransactionConfiguration::class],
    extraIncludes = ["config", "infrastructure"]
)
abstract class EmailModuleTestTemplate

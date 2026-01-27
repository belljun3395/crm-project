package com.manage.crm.event

import com.manage.crm.config.RedisKafkaContainerInitializer
import com.manage.crm.config.TestTransactionConfiguration
import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@TestPropertySource(properties = ["message.provider=kafka", "scheduler.provider=redis-kafka"])
@ActiveProfiles(value = ["test", "new"])
@EnableAutoConfiguration(
    exclude = [FlywayAutoConfiguration::class, SqsAutoConfiguration::class]
)
@ContextConfiguration(initializers = [RedisKafkaContainerInitializer::class])
@ApplicationModuleTest(
    module = "event",
    mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES,
    classes = [TestTransactionConfiguration::class],
    extraIncludes = ["config", "infrastructure"]
)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores::class)
abstract class EventModuleTestTemplate

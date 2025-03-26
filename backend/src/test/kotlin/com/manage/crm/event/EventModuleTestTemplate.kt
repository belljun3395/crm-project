package com.manage.crm.event

import com.manage.crm.config.TestTransactionConfiguration
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = ["test", "new"])
@EnableAutoConfiguration(
    exclude = [FlywayAutoConfiguration::class]
)
@ApplicationModuleTest(
    module = "event",
    mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES,
    classes = [TestTransactionConfiguration::class],
    extraIncludes = ["config"]
)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores::class)
abstract class EventModuleTestTemplate

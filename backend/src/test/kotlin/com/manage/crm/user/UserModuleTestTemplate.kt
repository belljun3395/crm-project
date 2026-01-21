package com.manage.crm.user

import com.manage.crm.config.TestTransactionConfiguration
import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = ["test", "new"])
@EnableAutoConfiguration(
    exclude = [FlywayAutoConfiguration::class, SqsAutoConfiguration::class]
)
@ApplicationModuleTest(
    module = "user",
    mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES,
    classes = [TestTransactionConfiguration::class],
    extraIncludes = ["config"]
)
abstract class UserModuleTestTemplate

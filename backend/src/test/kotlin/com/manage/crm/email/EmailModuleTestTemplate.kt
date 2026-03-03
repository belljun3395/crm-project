package com.manage.crm.email

import com.manage.crm.config.TestTransactionConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor

@ActiveProfiles(value = ["test", "new"])
@EnableAutoConfiguration(
    exclude = [FlywayAutoConfiguration::class]
)
@SpringBootTest
@Import(TestTransactionConfiguration::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class EmailModuleTestTemplate

package com.manage.crm.event

import com.manage.crm.config.TestTransactionConfiguration
import com.manage.crm.integration.config.TestInfraSupport
import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestConstructor

@ActiveProfiles(value = ["test", "new"])
@EnableAutoConfiguration(
    exclude = [SqsAutoConfiguration::class]
)
@SpringBootTest
@Import(TestTransactionConfiguration::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores::class)
abstract class EventModuleTestTemplate {
    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun registerPostgresProperties(registry: DynamicPropertyRegistry) {
            TestInfraSupport.register(registry)
        }
    }
}

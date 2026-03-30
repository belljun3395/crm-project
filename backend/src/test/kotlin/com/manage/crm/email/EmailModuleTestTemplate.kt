package com.manage.crm.email

import com.manage.crm.config.TestTransactionConfiguration
import com.manage.crm.integration.config.TestInfraSupport
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestConstructor

@ActiveProfiles(value = ["test", "new"])
@EnableAutoConfiguration
@SpringBootTest
@Import(TestTransactionConfiguration::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class EmailModuleTestTemplate {
    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun registerPostgresProperties(registry: DynamicPropertyRegistry) {
            TestInfraSupport.register(registry)
        }
    }
}

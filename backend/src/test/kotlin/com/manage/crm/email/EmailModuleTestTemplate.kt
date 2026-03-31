package com.manage.crm.email

import com.manage.crm.integration.config.SimpleTestContainers
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@ActiveProfiles(value = ["test", "new"])
@EnableAutoConfiguration
abstract class EmailModuleTestTemplate : BehaviorSpec() {
    override fun extensions() = listOf(SpringExtension)
    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun configureTestInfra(registry: DynamicPropertyRegistry) {
            SimpleTestContainers.register(registry)
        }
    }
}

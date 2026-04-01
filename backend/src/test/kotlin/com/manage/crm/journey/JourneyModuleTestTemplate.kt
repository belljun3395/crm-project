package com.manage.crm.journey

import com.manage.crm.integration.config.SimpleTestContainers
import io.kotest.core.spec.style.BehaviorSpec
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@ActiveProfiles(value = ["test", "new"])
abstract class JourneyModuleTestTemplate : BehaviorSpec() {
    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun configureTestInfra(registry: DynamicPropertyRegistry) {
            SimpleTestContainers.register(registry)
        }
    }
}

package com.manage.crm.event

import com.manage.crm.integration.config.SimpleTestContainers
import io.kotest.core.spec.style.BehaviorSpec
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@ActiveProfiles(value = ["test", "new"])
abstract class EventModuleTestTemplate : BehaviorSpec() {
    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun configureTestInfra(registry: DynamicPropertyRegistry) {
            SimpleTestContainers.register(registry)
        }
    }
}

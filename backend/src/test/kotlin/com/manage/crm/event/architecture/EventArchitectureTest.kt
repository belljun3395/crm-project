package com.manage.crm.event.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withAllAnnotationsOf
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

class EventArchitectureTest {
    @Test
    fun `event use case classes reside in application package`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .filter { it.resideInPackage("..event..") }
            .assertTrue { it.resideInPackage("..event.application..") }
    }

    @Test
    fun `event component classes in application end with UseCase`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withAllAnnotationsOf(Component::class)
            .filter { it.resideInPackage("..event.application..") }
            .assertTrue { it.hasNameEndingWith("UseCase") }
    }

    @Test
    fun `event application does not use Service annotation`() {
        val applicationServiceBeans = Konsist
            .scopeFromProduction()
            .classes()
            .withAllAnnotationsOf(Service::class)
            .filter { it.resideInPackage("..event.application..") }

        assertTrue(applicationServiceBeans.isEmpty())
    }
}

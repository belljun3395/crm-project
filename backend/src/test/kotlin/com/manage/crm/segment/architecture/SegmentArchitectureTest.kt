package com.manage.crm.segment.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withAllAnnotationsOf
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

class SegmentArchitectureTest {
    @Test
    fun `segment use case classes reside in application package`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .filter { it.resideInPackage("..segment..") }
            .assertTrue { it.resideInPackage("..segment.application..") }
    }

    @Test
    fun `segment component classes in application end with UseCase`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withAllAnnotationsOf(Component::class)
            .filter { it.resideInPackage("..segment.application..") }
            .assertTrue { it.hasNameEndingWith("UseCase") }
    }

    @Test
    fun `segment application does not use Service annotation`() {
        val applicationServiceBeans = Konsist
            .scopeFromProduction()
            .classes()
            .withAllAnnotationsOf(Service::class)
            .filter { it.resideInPackage("..segment.application..") }

        assertTrue(applicationServiceBeans.isEmpty())
    }
}

package com.manage.crm.journey.architecture

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController

class JourneyConventionTest : BehaviorSpec({
    given("journey module conventions") {
        `when`("checking controller dependencies") {
            val controllers = scanAnnotatedClasses("com.manage.crm.journey.controller", RestController::class.java)

            then("controllers depend only on journey use cases") {
                controllers.forEach { controllerClass ->
                    constructorDependencies(controllerClass).forEach { dependencyType ->
                        dependencyType.packageName.startsWith("com.manage.crm.journey.application").shouldBeTrue()
                    }
                }
            }
        }

        `when`("checking use case shape") {
            val useCases = scanAnnotatedClasses("com.manage.crm.journey.application", Component::class.java)
            val serviceAnnotatedApplicationBeans =
                scanAnnotatedClasses("com.manage.crm.journey.application", Service::class.java)

            then("all use case classes provide execute entrypoint") {
                useCases
                    .distinctBy { it.name }
                    .filter { it.simpleName.endsWith("UseCase") }
                    .forEach { useCaseClass ->
                        useCaseClass.methods.any { it.name == "execute" }.shouldBe(true)
                    }
            }

            then("application beans are declared as UseCase") {
                useCases
                    .distinctBy { it.name }
                    .forEach { applicationBeanClass ->
                        applicationBeanClass.simpleName.endsWith("UseCase").shouldBeTrue()
                    }
            }

            then("application use cases use Component annotation only") {
                serviceAnnotatedApplicationBeans.shouldBe(emptyList())
            }
        }

        `when`("checking dependency direction to prevent cyclic references") {
            val applicationBeans = scanAnnotatedClasses("com.manage.crm.journey.application", Component::class.java)

            then("application layer does not depend on controller layer") {
                applicationBeans.forEach { applicationClass ->
                    constructorDependencies(applicationClass).forEach { dependencyType ->
                        dependencyType.packageName.startsWith("com.manage.crm.journey.controller").shouldBe(false)
                    }
                }
            }
        }
    }
})

private fun scanAnnotatedClasses(basePackage: String, annotationClass: Class<out Annotation>): List<Class<*>> {
    val scanner = ClassPathScanningCandidateComponentProvider(false)
    scanner.addIncludeFilter(AnnotationTypeFilter(annotationClass))
    return scanner.findCandidateComponents(basePackage)
        .mapNotNull { it.beanClassName }
        .map { Class.forName(it) }
}

private fun constructorDependencies(clazz: Class<*>): List<Class<*>> {
    val primaryConstructor = clazz.declaredConstructors
        .filterNot { constructor ->
            constructor.isSynthetic ||
                constructor.parameterTypes.any { it.name == "kotlin.jvm.internal.DefaultConstructorMarker" }
        }
        .maxByOrNull { it.parameterCount }
    return primaryConstructor?.parameterTypes?.toList().orEmpty()
}

package com.manage.crm.segment.architecture

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController

class SegmentConventionTest : BehaviorSpec({
    given("segment module conventions") {
        `when`("checking controller dependencies") {
            val controllers = scanAnnotatedClasses("com.manage.crm.segment.controller", RestController::class.java)

            then("controllers depend only on segment use cases") {
                controllers.forEach { controllerClass ->
                    constructorDependencies(controllerClass).forEach { dependencyType ->
                        dependencyType.packageName.startsWith("com.manage.crm.segment.application").shouldBeTrue()
                    }
                }
            }
        }

        `when`("checking service role definition") {
            val services = scanAnnotatedClasses("com.manage.crm.segment.service", Service::class.java)

            then("each service coordinates multiple collaborators") {
                services.forEach { serviceClass ->
                    val dependencies = constructorDependencies(serviceClass)
                    dependencies.size.shouldBeGreaterThanOrEqual(2)
                }
            }
        }

        `when`("checking use case shape") {
            val useCases = scanAnnotatedClasses("com.manage.crm.segment.application", Component::class.java)
            val serviceAnnotatedApplicationBeans =
                scanAnnotatedClasses("com.manage.crm.segment.application", Service::class.java)

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
            val applicationBeans = scanAnnotatedClasses("com.manage.crm.segment.application", Component::class.java)
            val services = scanAnnotatedClasses("com.manage.crm.segment.service", Service::class.java)

            then("application layer does not depend on controller layer") {
                applicationBeans.forEach { applicationClass ->
                    constructorDependencies(applicationClass).forEach { dependencyType ->
                        dependencyType.packageName.startsWith("com.manage.crm.segment.controller").shouldBe(false)
                    }
                }
            }

            then("service layer does not depend on application/controller layer") {
                services.forEach { serviceClass ->
                    constructorDependencies(serviceClass).forEach { dependencyType ->
                        dependencyType.packageName.startsWith("com.manage.crm.segment.application").shouldBe(false)
                        dependencyType.packageName.startsWith("com.manage.crm.segment.controller").shouldBe(false)
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

import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.kapt)

    /** test */
    id("java-test-fixtures")

    /** spring */
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)

    /** docs */
    alias(libs.plugins.springdoc.openapi)

    /** lint */
    alias(libs.plugins.ktlint)

    /** coverage */
    id("jacoco")
}

fun bomCoordinate(dependency: Provider<MinimalExternalModuleDependency>): String {
    val module = dependency.get()
    return "${module.module.group}:${module.module.name}:${module.versionConstraint.requiredVersion}"
}

group = "com.manage"
version = "0.0.1-SNAPSHOT"
val jvmVersion = libs.versions.java.get()
val jvmVersionInt = jvmVersion.toInt()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersionInt)
    }
}

repositories {
    mavenCentral()
}

dependencyLocking {
    lockAllConfigurations()
}

/**
 * https://kotlinlang.org/docs/reference/compiler-plugins.html#spring-support
 * automatically supported annotation
 * @Component, @Async, @Transactional, @Cacheable, @SpringBootTest,
 * @Configuration, @Controller, @RestController, @Service, @Repository.
 * jpa meta-annotations not automatically opened through the default settings of the plugin.spring
 */
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
    annotation("org.springframework.data.relational.core.mapping.Table")
}

dependencyManagement {
    imports {
        mavenBom(bomCoordinate(libs.spring.modulith.bom))
        mavenBom(bomCoordinate(libs.spring.cloud.dependencies.bom))
        mavenBom(bomCoordinate(libs.aws.sdk.bom))
    }
}

dependencies {
    /** kotlin */
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.slf4j)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.reactor.kotlin.extensions)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jooq)

    /** spring */
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.r2dbc)
    implementation(libs.r2dbc.pool)
    implementation(libs.spring.boot.starter.data.redis.reactive)
    implementation(libs.spring.kafka)
    implementation(libs.spring.cloud.starter.circuitbreaker.reactor.resilience4j)
    implementation(libs.spring.modulith.starter.core)
    runtimeOnly(libs.spring.modulith.actuator)
    runtimeOnly(libs.spring.modulith.observability)
    implementation(libs.spring.boot.starter.mail)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.springdoc.openapi.starter.webflux.ui)

    /** data */
    runtimeOnly(libs.postgresql)
    implementation(libs.r2dbc.postgresql)
    testRuntimeOnly(libs.postgresql)
    testImplementation(libs.hikari.cp)

    /** commons-io */
    implementation(libs.commons.io)

    /** flyway */
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    /** domain */
    implementation(libs.jmolecules.starter.ddd)

    /** arrow */
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)

    /** aspectj */
    implementation(libs.aspectjweaver)

    /** aws */
    implementation(libs.spring.cloud.aws.starter.sqs)
    implementation(libs.aws.sdk.sqs)
    implementation(libs.aws.sdk.sns)
    implementation(libs.aws.sdk.ses)
    implementation(libs.aws.sdk.scheduler)
    implementation(libs.aws.query.protocol)

    /** docs */
    runtimeOnly(libs.therapi.runtime.javadoc.scribe)
    kapt(libs.therapi.runtime.javadoc.scribe)

    /** jsoup */
    implementation(libs.jsoup)

    /** monitoring */
    runtimeOnly(libs.micrometer.registry.prometheus)
    implementation(libs.loki.logback.appender)

    /** test */
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.modulith.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.framework.api)
    testImplementation(libs.kotest.extensions.spring)
    testImplementation(libs.kotest.extensions.allure)
    testImplementation(libs.konsist)
    testImplementation(libs.kotlinx.coroutines.test)

    /** testcontainers */
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.localstack)
    testImplementation(libs.testcontainers)

    /** logger */
    implementation(libs.kotlin.logging.jvm)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(jvmVersion))
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(jvmVersionInt)
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("api.version", System.getProperty("api.version") ?: "1.44")
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

openApi {
    waitTimeInSeconds.set(90)
    customBootRun {
        jvmArgs = listOf("-Dspring.profiles.active=local,new,openapi")
    }
}

springBoot {
    mainClass.set("com.manage.crm.CrmApplicationKt")
    buildInfo {
        properties { }
    }
}

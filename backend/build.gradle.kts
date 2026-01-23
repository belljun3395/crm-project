plugins {
    kotlin("jvm") version DependencyVersion.KOTLIN
    kotlin("plugin.spring") version DependencyVersion.KOTLIN
    kotlin("plugin.allopen") version DependencyVersion.KOTLIN
    kotlin("kapt") version DependencyVersion.KOTLIN

    /** test */
    id("java-test-fixtures")

    /** spring */
    id("org.springframework.boot") version DependencyVersion.SPRING_BOOT
    id("io.spring.dependency-management") version DependencyVersion.SPRING_DEPENDENCY_MANAGEMENT

    /** docs */
    id("org.springdoc.openapi-gradle-plugin") version DependencyVersion.SPRING_OPENAPI

    /** lint */
    id("org.jlleitschuh.gradle.ktlint") version DependencyVersion.KTLINT
}

group = "com.manage"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
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
        mavenBom("org.springframework.modulith:spring-modulith-bom:${DependencyVersion.SPRING_MODULITH}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${DependencyVersion.SPRING_CLOUD}")
        mavenBom("software.amazon.awssdk:bom:${DependencyVersion.AWS_SDK}")
    }
}

dependencies {
    /** kotlin */
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    /** spring */
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("io.r2dbc:r2dbc-pool")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    runtimeOnly("org.springframework.modulith:spring-modulith-actuator")
    runtimeOnly("org.springframework.modulith:spring-modulith-observability")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:${DependencyVersion.SPRINGDOC}")

    /** data */
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("io.asyncer:r2dbc-mysql")
    testRuntimeOnly("com.mysql:mysql-connector-j")
    testImplementation("com.zaxxer:HikariCP:${DependencyVersion.HIKARI_CP}")

    /** commons-io */
    implementation("commons-io:commons-io:${DependencyVersion.COMMONS_IO}")

    /** flyway */
    implementation("org.flywaydb:flyway-core:${DependencyVersion.FLYWAY}")
    implementation("org.flywaydb:flyway-mysql")

    /** domain */
    implementation("org.jmolecules.integrations:jmolecules-starter-ddd:${DependencyVersion.JMOLECULES}")

    /** arrow */
    implementation("io.arrow-kt:arrow-core:${DependencyVersion.ARROW}")
    implementation("io.arrow-kt:arrow-fx-coroutines:${DependencyVersion.ARROW}")

    /** aspectj */
    implementation("org.aspectj:aspectjweaver:${DependencyVersion.ASPECTJ}")

    /** aws */
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs:${DependencyVersion.AWS_SQS}")
    implementation("software.amazon.awssdk:sqs")
    implementation("software.amazon.awssdk:sns")
    implementation("software.amazon.awssdk:scheduler")
    implementation("com.amazonaws:aws-java-sdk-ses:${DependencyVersion.AWS_SES}")
    implementation("software.amazon.awssdk:aws-query-protocol")

    /** docs */
    runtimeOnly("com.github.therapi:therapi-runtime-javadoc-scribe:${DependencyVersion.JAVADOC_SCRIBE}")
    kapt("com.github.therapi:therapi-runtime-javadoc-scribe:${DependencyVersion.JAVADOC_SCRIBE}")

    /** jsoup */
    implementation("org.jsoup:jsoup:${DependencyVersion.JSOUP}")

    /** monitoring */
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("com.github.loki4j:loki-logback-appender:1.4.2")

    /** test */
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:${DependencyVersion.MOCKK}")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${DependencyVersion.MOCKITO_KOTLIN}")
    testImplementation("io.kotest:kotest-runner-junit5:${DependencyVersion.KOTEST}")
    testImplementation("io.kotest:kotest-assertions-core:${DependencyVersion.KOTEST}")
    testImplementation("io.kotest:kotest-framework-api:${DependencyVersion.KOTEST}")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:${DependencyVersion.KOTEST_EXTENSION}")
    testImplementation("io.kotest.extensions:kotest-extensions-allure:${DependencyVersion.KOTEST_EXTENSION}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${DependencyVersion.COROUTINE_TEST}")

    /** testcontainers */
    testImplementation("org.testcontainers:junit-jupiter:${DependencyVersion.TESTCONTAINERS}")
    testImplementation("org.testcontainers:mysql:${DependencyVersion.TESTCONTAINERS}")
    testImplementation("org.testcontainers:testcontainers:${DependencyVersion.TESTCONTAINERS}")

    /** logger */
    implementation("io.github.oshai:kotlin-logging-jvm:${DependencyVersion.KOTLIN_LOGGING}")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

ktlint {
    disabledRules.set(setOf("filename"))
}

openApi {
    customBootRun {
        jvmArgs = listOf("-Dspring.profiles.active=local,new")
    }
}

springBoot.buildInfo { properties { } }

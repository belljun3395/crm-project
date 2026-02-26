package com.manage.crm.infrastructure.gcp.validation

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
@Profile("!test & !openapi")
@ConditionalOnProperty(name = ["cloud.provider"], havingValue = "gcp")
class GcpResourceValidator(
    private val env: Environment
) : ApplicationRunner {

    private val log = KotlinLogging.logger {}

    // Required GCP Pub/Sub properties
    private val requiredProperties = listOf(
        "gcp.pubsub.cache-invalidation-topic",
        "gcp.pubsub.cache-invalidation-subscription"
    )

    override fun run(args: ApplicationArguments?) {
        log.info { "Starting GCP resource validation..." }

        var validationFailed = false

        requiredProperties.forEach { property ->
            if (!validateProperty(property)) {
                validationFailed = true
            }
        }

        if (validationFailed) {
            log.error { "GCP resource validation failed. Please check the configuration." }
            throw IllegalStateException("GCP resource validation failed")
        } else {
            log.info { "GCP resource validation completed successfully." }
        }
    }

    private fun validateProperty(property: String): Boolean {
        val value = env.getProperty(property)
        return if (value.isNullOrBlank()) {
            log.error { "✗ Required GCP property is missing or blank: $property" }
            false
        } else {
            log.info { "✓ GCP property validated: $property = $value" }
            true
        }
    }
}

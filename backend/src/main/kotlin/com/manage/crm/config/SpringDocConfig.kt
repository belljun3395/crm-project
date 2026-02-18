package com.manage.crm.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.web.reactive.result.view.RequestContext
import org.springframework.web.server.WebSession
import java.util.*

/**
 * **Swagger**
 *
 * Provide detailed explanations based on comments.
 *
 * [Local Swagger UI](http://localhost:8080/webjars/swagger-ui/index.html)
 */
@Configuration
@Profile("!test")
class SpringDocConfig(
    private val buildProperties: BuildProperties,
    private val environment: Environment
) {
    companion object {
        const val AUTH_TOKEN_KEY = "Authorization"
    }

    init {
        SpringDocUtils
            .getConfig()
            .addRequestWrapperToIgnore(
                WebSession::class.java,
                RequestContext::class.java
            )
    }

    @Bean
    fun openApi(): OpenAPI {
        val profiles = environment.activeProfiles.contentToString()
        val url = if (profiles.contains("local")) "http://localhost:8080" else "https://api.example.com"

        val securityRequirement = SecurityRequirement().addList(AUTH_TOKEN_KEY)
        return OpenAPI()
            .components(authSetting())
            .security(listOf(securityRequirement))
            .addServersItem(Server().url(url))
            .info(
                Info()
                    .title(buildProperties.name)
                    .version(buildProperties.version)
                    .description("${buildProperties.name.uppercase(Locale.getDefault())} API Docs")
            )
    }

    @Bean
    fun idempotencyOpenApiCustomizer(): OpenApiCustomizer {
        return OpenApiCustomizer { openApi ->
            val postPaths = setOf(
                "/api/v1/users",
                "/api/v1/events",
                "/api/v1/events/campaign",
                "/api/v1/emails/send/notifications",
                "/api/v1/emails/schedules/notifications/email",
                "/api/v1/webhooks"
            )

            openApi.paths?.forEach { (path, pathItem) ->
                if (path in postPaths) {
                    addIdempotencyKeyParameter(pathItem.post)
                }
                if (path == "/api/v1/webhooks/{id}") {
                    addIdempotencyKeyParameter(pathItem.put)
                }
            }
        }
    }

    private fun authSetting(): Components {
        return Components()
            .addSecuritySchemes(
                AUTH_TOKEN_KEY,
                SecurityScheme()
                    .description("Access Token")
                    .type(SecurityScheme.Type.APIKEY)
                    .`in`(SecurityScheme.In.HEADER)
                    .name(AUTH_TOKEN_KEY)
            )
    }

    private fun addIdempotencyKeyParameter(operation: io.swagger.v3.oas.models.Operation?) {
        if (operation == null) {
            return
        }

        val exists = operation.parameters?.any { it.name == "Idempotency-Key" && it.`in` == "header" } == true
        if (exists) {
            return
        }

        val parameter = Parameter()
            .name("Idempotency-Key")
            .`in`("header")
            .required(true)
            .description("Idempotency key for write request replay and duplicate prevention")

        operation.parameters = (operation.parameters ?: mutableListOf()).toMutableList().apply {
            add(parameter)
        }
    }
}

object SwaggerTag {
    const val USERS_SWAGGER_TAG = "Users API"
    const val EMAILS_SWAGGER_TAG = "Emails API"
    const val EVENT_SWAGGER_TAG = "Event API"
    const val WEBHOOKS_SWAGGER_TAG = "Webhooks API"
}

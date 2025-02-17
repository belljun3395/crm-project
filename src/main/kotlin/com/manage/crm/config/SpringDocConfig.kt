package com.manage.crm.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
}

object SwaggerTag {
    const val USERS_SWAGGER_TAG = "Users API"
}

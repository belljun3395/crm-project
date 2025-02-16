package com.manage.crm.config

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.StringTemplateResolver

@Configuration
@EnableAutoConfiguration(exclude = [ThymeleafAutoConfiguration::class])
class ThymeleafConfig {
    companion object {
        const val HTML_TEMPLATE_ENGINE = "htmlTemplateEngine"
        const val STRING_TEMPLATE_ENGINE = "stringTemplateEngine"
        const val SPRING_RESOURCE_TEMPLATE_RESOLVER = "springResourceTemplateResolver"
        const val STRING_TEMPLATE_RESOLVER = "stringTemplateResolver"
    }

    @Bean(name = [HTML_TEMPLATE_ENGINE])
    fun htmlTemplateEngine(): SpringTemplateEngine {
        val templateEngine = SpringWebFluxTemplateEngine()
        templateEngine.addTemplateResolver(springResourceTemplateResolver())
        return templateEngine
    }

    @Bean(name = [STRING_TEMPLATE_ENGINE])
    fun stringTemplateEngine(): SpringTemplateEngine {
        val templateEngine = SpringWebFluxTemplateEngine()
        templateEngine.addTemplateResolver(stringTemplateResolver())
        return templateEngine
    }

    @Bean(name = [SPRING_RESOURCE_TEMPLATE_RESOLVER])
    fun springResourceTemplateResolver(): SpringResourceTemplateResolver {
        val resolver = SpringResourceTemplateResolver()
        resolver.order = 1
        resolver.prefix = "classpath:templates/"
        resolver.suffix = ".html"
        resolver.templateMode = TemplateMode.HTML
        resolver.characterEncoding = "UTF-8"
        resolver.isCacheable = false
        return resolver
    }

    @Bean(name = [STRING_TEMPLATE_RESOLVER ])
    fun stringTemplateResolver(): StringTemplateResolver {
        val stringTemplateResolver = StringTemplateResolver()
        stringTemplateResolver.setTemplateMode(TemplateMode.HTML.name)
        stringTemplateResolver.isCacheable = false
        return stringTemplateResolver
    }
}

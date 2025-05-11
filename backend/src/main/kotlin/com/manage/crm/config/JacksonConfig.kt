package com.manage.crm.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.hash.Jackson2HashMapper
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class JacksonConfig {
    /**
     * this Bean can customize spring boot auto-configured ObjectMapper.
     * you can also customize via yml configuration. spring.jackson.x
     */
    @Bean
    fun customizeJson(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { builder: Jackson2ObjectMapperBuilder ->
            builder
                .failOnUnknownProperties(false)
                .serializationInclusion(JsonInclude.Include.NON_ABSENT)
        }
    }

    @Bean
    fun jackson2HashMapper(objectMapper: ObjectMapper): Jackson2HashMapper {
        return Jackson2HashMapper(objectMapper, true)
    }
}

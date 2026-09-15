package cn.arorms.llm.router.app.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.JacksonModule
import java.util.ServiceLoader

/**
 * Jackson 2 remains for gateway adapters and upstream protocol handling.
 * Spring MVC uses Jackson 3, so its Kotlin module must be registered separately.
 */
@Configuration
class JacksonConfig {
    @Bean
    fun jackson2ObjectMapper(): ObjectMapper = jacksonObjectMapper()
        .findAndRegisterModules()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    /**
     * Load through Jackson's ServiceLoader entry instead of referencing
     * KotlinModule.Builder directly. This also avoids IDE runtime classpaths
     * built from an older module release.
     */
    @Bean
    fun jackson3KotlinModule(): JacksonModule =
        ServiceLoader.load(JacksonModule::class.java)
            .firstOrNull()
            ?: error("Jackson 3 Kotlin module was not found on the runtime classpath")

    @Bean
    fun jackson3ObjectMapperCustomizer(): JsonMapperBuilderCustomizer = JsonMapperBuilderCustomizer { builder ->
        builder.configure(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}

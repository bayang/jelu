package io.github.bayang.jelu.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(private val properties: JeluProperties) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/files/**")
            .addResourceLocations(getExternalFilesFolderPath())
    }

    fun getExternalFilesFolderPath(): String {
        var suffix: String = if (properties.files.dir.endsWith("/")) { "" } else { "/" }
        return "file:" + properties.files.dir + suffix
    }
}

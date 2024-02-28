package io.github.bayang.jelu.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.concurrent.TimeUnit

const val EXPORTS_PREFIX = "/exports"

@Configuration
class WebMvcConfig(private val properties: JeluProperties) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // serve pictures
        registry.addResourceHandler("/files/**")
            .addResourceLocations(getExternalFilesFolderPath())
            .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())

        // serve export csv
        registry.addResourceHandler("$EXPORTS_PREFIX/**")
            .addResourceLocations(getExternalExportsFolderPath())
            .setCacheControl(CacheControl.noCache())

        registry.addResourceHandler("/assets/**")
            .addResourceLocations("classpath:public/assets/")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())

        registry
            .addResourceHandler(
                "/index.html",
                "/favicon.ico",
                "/favicon-16x16.png",
                "/favicon-32x32.png",
                "/mstile-144x144.png",
                "/apple-touch-icon.png",
                "/apple-touch-icon-180x180.png",
                "/android-chrome-192x192.png",
                "/android-chrome-512x512.png",
                "/manifest.json",
                "/registerSW.js",
                "/sw.js",
                "/manifest.webmanifest",
                "/site.webmanifest",
            )
            .addResourceLocations(
                "classpath:public/index.html",
                "classpath:public/favicon.ico",
                "classpath:public/favicon-16x16.png",
                "classpath:public/favicon-32x32.png",
                "classpath:public/mstile-144x144.png",
                "classpath:public/apple-touch-icon.png",
                "classpath:public/apple-touch-icon-180x180.png",
                "classpath:public/android-chrome-192x192.png",
                "classpath:public/android-chrome-512x512.png",
                "classpath:public/manifest.json",
                "classpath:public/registerSW.js",
                "classpath:public/sw.js",
                "classpath:public/manifest.webmanifest",
                "classpath:public/site.webmanifest",
            )
            .setCacheControl(CacheControl.noStore())
    }

    fun getExternalFilesFolderPath(): String {
        var suffix: String = if (properties.files.images.endsWith("/")) { "" } else { "/" }
        return "file:" + properties.files.images + suffix
    }

    fun getExternalExportsFolderPath(): String {
        var suffix: String = if (properties.files.imports.endsWith("/")) { "" } else { "/" }
        return "file:" + properties.files.imports + suffix
    }
}

@ControllerAdvice
class Customizer {
    @ExceptionHandler(NoHandlerFoundException::class)
    fun notFound(): String {
        return "forward:/"
    }
}

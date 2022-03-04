package io.github.bayang.jelu

import io.github.bayang.jelu.config.JeluProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableConfigurationProperties(JeluProperties::class)
@EnableAsync
@EnableScheduling
class JeluApplication

fun main(args: Array<String>) {
    runApplication<JeluApplication>(*args)
}

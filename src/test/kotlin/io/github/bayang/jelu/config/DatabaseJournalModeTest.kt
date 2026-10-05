package io.github.bayang.jelu.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertySources
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.sql.DriverManager

/**
 * The test profile swaps the datasource for :memory:, where journal_mode is a no-op, so we bind the url
 * shipped in application.yml the same way DataSourceProperties would and open a real file with it.
 */
class DatabaseJournalModeTest {
    @TempDir
    lateinit var dbDir: File

    @ParameterizedTest(name = "{0}={1} gives {2}")
    @CsvSource(
        "'', '', delete",
        "jelu.database.journalMode, wal, wal",
        "jelu.database.journal-mode, WAL, wal",
        "JELU_DATABASE_JOURNALMODE, wal, wal",
        "jelu.database.journal-mode, delete, delete",
    )
    fun `applies the configured journal mode`(
        key: String,
        value: String,
        expected: String,
    ) {
        // Start from the other mode so that a setting which never reaches sqlite can't pass by accident
        val other = if (expected == "wal") "delete" else "wal"
        assertEquals(other, journalModeOf("jdbc:sqlite:${File(dbDir, "jelu.db")}?journal_mode=$other"))

        val override =
            when {
                key.isEmpty() -> null
                key == key.uppercase() ->
                    SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mapOf(key to value))
                else -> MapPropertySource("userConfig", mapOf(key to value))
            }
        assertEquals(expected, journalModeOf(shippedUrl(override)))
    }

    private fun shippedUrl(override: PropertySource<*>?): String {
        val env = StandardEnvironment()
        val sources = env.propertySources
        // We don't want anything set on the machine running the tests leaking in
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)
        override?.let { sources.addLast(it) }
        sources.addLast(MapPropertySource("dbDir", mapOf("jelu.database.path" to dbDir.absolutePath)))
        YamlPropertySourceLoader()
            .load("application.yml", ClassPathResource("application.yml"))
            .forEach { sources.addLast(it) }
        ConfigurationPropertySources.attach(env)
        return Binder.get(env).bind("spring.datasource.url", String::class.java).get()
    }

    private fun journalModeOf(url: String): String =
        DriverManager.getConnection(url).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("PRAGMA journal_mode").use { rs ->
                    rs.next()
                    rs.getString(1)
                }
            }
        }
}

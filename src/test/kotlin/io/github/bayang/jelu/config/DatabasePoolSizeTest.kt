package io.github.bayang.jelu.config

import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import javax.sql.DataSource

/**
 * Each case needs its own context because the pool is sized once at startup, which is why these
 * are separate classes rather than a parameterized test.
 */
@SpringBootTest
class DatabasePoolSizeTest(
    @Autowired private val dataSource: DataSource,
    @Autowired private val jeluProperties: JeluProperties,
) {
    @Test
    fun `uses a single connection under the default delete journal`() {
        assertNull(jeluProperties.database.poolSize)
        assertEquals(1, (dataSource as HikariDataSource).maximumPoolSize)
    }
}

@SpringBootTest
@TestPropertySource(properties = ["jelu.database.journalMode=wal"])
class DatabasePoolSizeWalTest(
    @Autowired private val dataSource: DataSource,
) {
    @Test
    fun `keeps hikari's usual ten under wal`() {
        assertEquals(10, (dataSource as HikariDataSource).maximumPoolSize)
    }
}

/**
 * Asks for a size neither journal mode would pick, so reaching the pool at all proves
 * jelu.database.poolSize is wired up.
 */
@SpringBootTest
@TestPropertySource(properties = ["jelu.database.poolSize=4", "jelu.database.journalMode=wal"])
class DatabasePoolSizeOverrideTest(
    @Autowired private val dataSource: DataSource,
) {
    @Test
    fun `honours a user supplied pool size over the journal mode default`() {
        assertEquals(4, (dataSource as HikariDataSource).maximumPoolSize)
    }
}

@SpringBootTest
@TestPropertySource(properties = ["jelu.database.pool-size=3"])
class DatabasePoolSizeKebabCaseTest(
    @Autowired private val dataSource: DataSource,
) {
    @Test
    fun `honours a kebab-case pool size`() {
        assertEquals(3, (dataSource as HikariDataSource).maximumPoolSize)
    }
}

@SpringBootTest
@TestPropertySource(properties = ["spring.datasource.hikari.maximum-pool-size=5", "jelu.database.poolSize=4"])
class DatabasePoolSizeHikariTest(
    @Autowired private val dataSource: DataSource,
) {
    @Test
    fun `lets hikari's own setting win`() {
        assertEquals(5, (dataSource as HikariDataSource).maximumPoolSize)
    }
}

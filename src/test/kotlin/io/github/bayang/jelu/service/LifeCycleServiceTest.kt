package io.github.bayang.jelu.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LifeCycleServiceTest(
    @Autowired private val lifeCycleService: LifeCycleService,
) {
    @Test
    fun testGetLifeCycle() {
        var lifeCycle = lifeCycleService.getLifeCycle()
        Assertions.assertNotNull(lifeCycle)
        Assertions.assertEquals(1000, lifeCycle.id)
        Assertions.assertTrue(lifeCycle.seriesMigrated)

        lifeCycle = lifeCycleService.setSeriesMigrated()
        Assertions.assertEquals(1000, lifeCycle.id)
        Assertions.assertTrue(lifeCycle.seriesMigrated)
    }
}

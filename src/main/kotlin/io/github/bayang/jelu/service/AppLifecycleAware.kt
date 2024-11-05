package io.github.bayang.jelu.service

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.search.LuceneHelper
import mu.KotlinLogging
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

@Component
class AppLifecycleAware(
    private val properties: JeluProperties,
    private val luceneHelper: LuceneHelper,
    private val searchIndexService: SearchIndexService,
    private val lifeCycleService: LifeCycleService,
    private val bookService: BookService,
) {

    @EventListener
    fun onApplicationEvent(event: ContextRefreshedEvent?) {
        val assetsDir = File(properties.files.images)
        if (!assetsDir.exists()) {
            val created = assetsDir.mkdirs()
            logger.debug { "Attempt to create non existing assets dir succeeded : $created" }
        }
        val importsDir = File(properties.files.imports)
        if (!importsDir.exists()) {
            val created = importsDir.mkdirs()
            logger.debug { "Attempt to create non existing imports dir succeeded : $created" }
        }
        if (!luceneHelper.indexExists()) {
            logger.info { "Lucene index not found, trigger rebuild" }
            searchIndexService.rebuildIndex()
        } else {
            val indexVersion = luceneHelper.getIndexVersion()
            logger.info { "Lucene index version: $indexVersion" }
            if (indexVersion < INDEX_VERSION) {
                searchIndexService.upgradeIndex()
                searchIndexService.rebuildIndex()
            }
        }
        val lifeCycle = lifeCycleService.getLifeCycle()
        if (!lifeCycle.seriesMigrated) {
            val start = System.currentTimeMillis()
            val nowString: String = OffsetDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
            logger.info { "Series data not migrated, starting migration at at $nowString" }
            bookService.migrateSeries()
            val end = System.currentTimeMillis()
            val deltaInSec = (end - start) / 1000
            logger.info { "Series data migration completed after : $deltaInSec seconds, check your data" }
            val seriesMigrated = lifeCycleService.setSeriesMigrated()
            logger.debug { "lifeCycled updated to ${seriesMigrated.seriesMigrated}" }
        }
    }
}

package io.github.bayang.jelu.service.metadata

import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.dto.MetadataRequestDto
import io.github.bayang.jelu.dto.PluginInfo
import io.github.bayang.jelu.service.metadata.providers.CalibreMetadataProvider
import io.github.bayang.jelu.service.metadata.providers.DebugMetadataProvider
import io.github.bayang.jelu.service.metadata.providers.IMetaDataProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.Optional

class FetchMetadataServiceTest {
    @Test
    fun fetchMetadataTest() {
        val jeluProperties =
            JeluProperties(
                JeluProperties.Database(""),
                JeluProperties.Files("", "", true),
                JeluProperties.Session(1),
                JeluProperties.Cors(),
                JeluProperties.Metadata(JeluProperties.Calibre("/path", order = 1)),
                JeluProperties.Auth(JeluProperties.Ldap(), JeluProperties.Proxy()),
                listOf(JeluProperties.MetaDataProvider(PluginInfoHolder.JELU_DEBUG, true, "", order = 10)),
            )
        val providers = mutableListOf<IMetaDataProvider>()
        val jeluDebug = mockk<DebugMetadataProvider>()
        every { jeluDebug.name() } returns PluginInfoHolder.JELU_DEBUG
        every { jeluDebug.fetchMetadata(any(), any()) } returns Optional.empty()
        val calibre = mockk<CalibreMetadataProvider>()
        every { calibre.name() } returns PluginInfoHolder.CALIBRE
        every { calibre.fetchMetadata(any(), any()) } returns Optional.empty()
        providers.add(jeluDebug)
        providers.add(calibre)
        val info = PluginInfoHolder(jeluProperties)
        val service = FetchMetadataService(providers, info)
        service.fetchMetadata(MetadataRequestDto(isbn = "1566199093"))
        verify { jeluDebug.fetchMetadata(any(), any()) }
        verify { calibre.fetchMetadata(any(), any()) }
    }

    @Test
    fun fetchMetadataTestFilteredByCaller() {
        val jeluProperties =
            JeluProperties(
                JeluProperties.Database(""),
                JeluProperties.Files("", "", true),
                JeluProperties.Session(1),
                JeluProperties.Cors(),
                JeluProperties.Metadata(JeluProperties.Calibre("/path", order = 1)),
                JeluProperties.Auth(JeluProperties.Ldap(), JeluProperties.Proxy()),
                listOf(JeluProperties.MetaDataProvider(PluginInfoHolder.JELU_DEBUG, true, "", order = 10)),
            )
        val providers = mutableListOf<IMetaDataProvider>()
        val jeluDebug = mockk<DebugMetadataProvider>()
        every { jeluDebug.name() } returns PluginInfoHolder.JELU_DEBUG
        every { jeluDebug.fetchMetadata(any(), any()) } returns Optional.empty()
        val calibre = mockk<CalibreMetadataProvider>()
        every { calibre.name() } returns PluginInfoHolder.CALIBRE
        every { calibre.fetchMetadata(any(), any()) } returns Optional.empty()
        providers.add(jeluDebug)
        providers.add(calibre)
        val info = PluginInfoHolder(jeluProperties)
        val service = FetchMetadataService(providers, info)
        service.fetchMetadata(
            MetadataRequestDto(
                isbn = "1566199093",
                plugins =
                    listOf(
                        PluginInfo(
                            name = PluginInfoHolder.JELU_DEBUG,
                            order = 1,
                        ),
                    ),
            ),
        )
        verify { jeluDebug.fetchMetadata(any(), any()) }
        verify(exactly = 0) { calibre.fetchMetadata(any(), any()) }
    }

    @Test
    fun fetchMetadataTestFilteredByCallerButListIsEmpty() {
        val jeluProperties =
            JeluProperties(
                JeluProperties.Database(""),
                JeluProperties.Files("", "", true),
                JeluProperties.Session(1),
                JeluProperties.Cors(),
                JeluProperties.Metadata(JeluProperties.Calibre("/path", order = 1)),
                JeluProperties.Auth(JeluProperties.Ldap(), JeluProperties.Proxy()),
                listOf(JeluProperties.MetaDataProvider(PluginInfoHolder.JELU_DEBUG, true, "", order = 10)),
            )
        val providers = mutableListOf<IMetaDataProvider>()
        val jeluDebug = mockk<DebugMetadataProvider>()
        every { jeluDebug.name() } returns PluginInfoHolder.JELU_DEBUG
        every { jeluDebug.fetchMetadata(any(), any()) } returns Optional.empty()
        val calibre = mockk<CalibreMetadataProvider>()
        every { calibre.name() } returns PluginInfoHolder.CALIBRE
        every { calibre.fetchMetadata(any(), any()) } returns Optional.empty()
        providers.add(jeluDebug)
        providers.add(calibre)
        val info = PluginInfoHolder(jeluProperties)
        val service = FetchMetadataService(providers, info)
        service.fetchMetadata(MetadataRequestDto(isbn = "1566199093", plugins = listOf()))
        verify { jeluDebug.fetchMetadata(any(), any()) }
        verify { calibre.fetchMetadata(any(), any()) }
    }

    @Test
    fun fetchMetadataTestFilteredByCallerButNoPluginExists() {
        val jeluProperties =
            JeluProperties(
                JeluProperties.Database(""),
                JeluProperties.Files("", "", true),
                JeluProperties.Session(1),
                JeluProperties.Cors(),
                JeluProperties.Metadata(JeluProperties.Calibre("/path", order = 1)),
                JeluProperties.Auth(JeluProperties.Ldap(), JeluProperties.Proxy()),
                listOf(JeluProperties.MetaDataProvider(PluginInfoHolder.JELU_DEBUG, true, "", order = 10)),
            )
        val providers = mutableListOf<IMetaDataProvider>()
        val jeluDebug = mockk<DebugMetadataProvider>()
        every { jeluDebug.name() } returns PluginInfoHolder.JELU_DEBUG
        every { jeluDebug.fetchMetadata(any(), any()) } returns Optional.empty()
        val calibre = mockk<CalibreMetadataProvider>()
        every { calibre.name() } returns PluginInfoHolder.CALIBRE
        every { calibre.fetchMetadata(any(), any()) } returns Optional.empty()
        providers.add(jeluDebug)
        providers.add(calibre)
        val info = PluginInfoHolder(jeluProperties)
        val service = FetchMetadataService(providers, info)
        service.fetchMetadata(
            MetadataRequestDto(
                isbn = "1566199093",
                plugins =
                    listOf(
                        PluginInfo(
                            name = "not-existing",
                            order = 1,
                        ),
                        PluginInfo(
                            name = "not-existing2",
                            order = 2,
                        ),
                    ),
            ),
        )
        verify(exactly = 0) { jeluDebug.fetchMetadata(any(), any()) }
        verify(exactly = 0) { calibre.fetchMetadata(any(), any()) }
    }

    @Test
    fun fetchMetadataTestFilteredByCallerButOnePluginDoesNotExists() {
        val jeluProperties =
            JeluProperties(
                JeluProperties.Database(""),
                JeluProperties.Files("", "", true),
                JeluProperties.Session(1),
                JeluProperties.Cors(),
                JeluProperties.Metadata(JeluProperties.Calibre("/path", order = 1)),
                JeluProperties.Auth(JeluProperties.Ldap(), JeluProperties.Proxy()),
                listOf(JeluProperties.MetaDataProvider(PluginInfoHolder.JELU_DEBUG, true, "", order = 10)),
            )
        val providers = mutableListOf<IMetaDataProvider>()
        val jeluDebug = mockk<DebugMetadataProvider>()
        every { jeluDebug.name() } returns PluginInfoHolder.JELU_DEBUG
        every { jeluDebug.fetchMetadata(any(), any()) } returns Optional.empty()
        val calibre = mockk<CalibreMetadataProvider>()
        every { calibre.name() } returns PluginInfoHolder.CALIBRE
        every { calibre.fetchMetadata(any(), any()) } returns Optional.empty()
        providers.add(jeluDebug)
        providers.add(calibre)
        val info = PluginInfoHolder(jeluProperties)
        val service = FetchMetadataService(providers, info)
        service.fetchMetadata(
            MetadataRequestDto(
                isbn = "1566199093",
                plugins =
                    listOf(
                        PluginInfo(
                            name = "not-existing",
                            order = 1,
                        ),
                        PluginInfo(
                            name = PluginInfoHolder.JELU_DEBUG,
                            order = 2,
                        ),
                    ),
            ),
        )
        verify { jeluDebug.fetchMetadata(any(), any()) }
        verify(exactly = 0) { calibre.fetchMetadata(any(), any()) }
    }
}

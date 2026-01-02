package io.github.bayang.jelu.service.metadata

import io.github.bayang.jelu.config.JeluProperties
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PluginInfoHolderTest {
    @Test
    fun testPluginsList() {
        var jeluProperties =
            JeluProperties(
                JeluProperties.Database(""),
                JeluProperties.Files("", "", true),
                JeluProperties.Session(1),
                JeluProperties.Cors(),
                JeluProperties.Metadata(JeluProperties.Calibre("")),
                JeluProperties.Auth(JeluProperties.Ldap(), JeluProperties.Proxy()),
                listOf(JeluProperties.MetaDataProvider("google", true, "fake-google-api-key")),
            )
        var info = PluginInfoHolder(jeluProperties)
        var plugins = info.plugins()
        Assertions.assertEquals(1, plugins.size)
        Assertions.assertEquals("google", plugins[0].name)
        Assertions.assertEquals(-1000, plugins[0].order)
        Assertions.assertFalse(info.calibreEnabled())

        jeluProperties =
            JeluProperties(
                JeluProperties.Database(""),
                JeluProperties.Files("", "", true),
                JeluProperties.Session(1),
                JeluProperties.Cors(),
                JeluProperties.Metadata(JeluProperties.Calibre("/path")),
                JeluProperties.Auth(JeluProperties.Ldap(), JeluProperties.Proxy()),
                listOf(JeluProperties.MetaDataProvider("google", true, "fake-google-api-key")),
            )
        info = PluginInfoHolder(jeluProperties)
        plugins = info.plugins()
        Assertions.assertEquals(2, plugins.size)
        Assertions.assertEquals(PluginInfoHolder.CALIBRE, plugins[0].name)
        Assertions.assertEquals(1000, plugins[0].order)
        Assertions.assertTrue(info.calibreEnabled())

        jeluProperties =
            JeluProperties(
                JeluProperties.Database(""),
                JeluProperties.Files("", "", true),
                JeluProperties.Session(1),
                JeluProperties.Cors(),
                JeluProperties.Metadata(JeluProperties.Calibre("/path", order = 1)),
                JeluProperties.Auth(JeluProperties.Ldap(), JeluProperties.Proxy()),
                listOf(JeluProperties.MetaDataProvider("google", true, "fake-google-api-key", order = 2)),
            )
        info = PluginInfoHolder(jeluProperties)
        plugins = info.plugins()
        Assertions.assertEquals(2, plugins.size)
        Assertions.assertEquals("google", plugins[0].name)
        Assertions.assertEquals(2, plugins[0].order)
        Assertions.assertTrue(info.calibreEnabled())

        jeluProperties =
            JeluProperties(
                JeluProperties.Database(""),
                JeluProperties.Files("", "", true),
                JeluProperties.Session(1),
                JeluProperties.Cors(),
                JeluProperties.Metadata(JeluProperties.Calibre("/path", order = 1)),
                JeluProperties.Auth(JeluProperties.Ldap(), JeluProperties.Proxy()),
                listOf(JeluProperties.MetaDataProvider("google", true, "fake-google-api-key", order = 1)),
            )
        info = PluginInfoHolder(jeluProperties)
        plugins = info.plugins()
        Assertions.assertEquals(2, plugins.size)
        Assertions.assertEquals(PluginInfoHolder.CALIBRE, plugins[0].name)
        Assertions.assertEquals(1, plugins[0].order)
        Assertions.assertTrue(info.calibreEnabled())
    }
}

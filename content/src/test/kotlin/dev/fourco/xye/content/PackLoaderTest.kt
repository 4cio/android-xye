package dev.fourco.xye.content

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class PackLoaderTest {
    private val loader = PackLoader()

    @Test
    fun `detectFormat identifies xye`() {
        assertEquals(LevelFormat.XYE, loader.detectFormat("test.xye"))
    }

    @Test
    fun `detectFormat identifies kye`() {
        assertEquals(LevelFormat.KYE, loader.detectFormat("test.kye"))
    }

    @Test
    fun `detectFormat identifies xsb`() {
        assertEquals(LevelFormat.XSB, loader.detectFormat("test.xsb"))
    }

    @Test
    fun `detectFormat identifies sok as xsb`() {
        assertEquals(LevelFormat.XSB, loader.detectFormat("test.sok"))
    }

    @Test
    fun `detectFormat is case insensitive`() {
        assertEquals(LevelFormat.XYE, loader.detectFormat("TEST.XYE"))
    }

    @Test
    fun `detectFormat throws on unknown`() {
        assertThrows<IllegalArgumentException> { loader.detectFormat("test.txt") }
    }

    @Test
    fun `loadFromResource loads xsb pack`() {
        val pack = loader.loadFromResource("testdata/minimal.xsb")
        assertEquals(LevelFormat.XSB, pack.format)
        assertTrue(pack.levels.isNotEmpty())
    }

    @Test
    fun `loadLevelFromResource loads xye level`() {
        val level = loader.loadLevelFromResource("testdata/teleport.xye", 0)
        assertNotNull(level.playerStart)
    }
}

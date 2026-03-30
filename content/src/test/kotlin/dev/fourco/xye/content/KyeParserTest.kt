package dev.fourco.xye.content

import dev.fourco.xye.engine.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class KyeParserTest {
    private val parser = KyeParser()

    private fun loadResource(name: String) =
        javaClass.classLoader.getResourceAsStream("testdata/$name")
            ?: throw IllegalStateException("Resource not found: testdata/$name")

    @Test
    fun `parsePack finds correct number of levels`() {
        val pack = parser.parsePack("classic", loadResource("classic.kye"))
        assertEquals(2, pack.levels.size)
    }

    @Test
    fun `parsePack extracts level names`() {
        val pack = parser.parsePack("classic", loadResource("classic.kye"))
        assertEquals("First Level", pack.levels[0].name)
        assertEquals("Second Level", pack.levels[1].name)
    }

    @Test
    fun `parseLevel has correct dimensions`() {
        val level = parser.parseLevel(loadResource("classic.kye"), 0)
        assertEquals(30, level.width)
        assertEquals(20, level.height)
    }

    @Test
    fun `parseLevel finds player`() {
        val level = parser.parseLevel(loadResource("classic.kye"), 0)
        assertEquals(Position(1, 1), level.playerStart)
    }

    @Test
    fun `parseLevel counts gems`() {
        val level = parser.parseLevel(loadResource("classic.kye"), 0)
        assertEquals(2, level.totalGems)
    }

    @Test
    fun `parseLevel finds walls`() {
        val level = parser.parseLevel(loadResource("classic.kye"), 0)
        val walls = level.entities.filter { it.kind == EntityKind.Wall }
        assertTrue(walls.isNotEmpty())
    }

    @Test
    fun `parseLevel second level has push block`() {
        val level = parser.parseLevel(loadResource("classic.kye"), 1)
        val blocks = level.entities.filter { it.kind == EntityKind.PushBlock }
        assertEquals(1, blocks.size)
    }

    @Test
    fun `toInitialState creates valid state`() {
        val level = parser.parseLevel(loadResource("classic.kye"), 0)
        val state = level.toInitialState()
        assertEquals(GameStatus.Playing, state.status)
        assertNotNull(state.player())
    }

    @Test
    fun `parseLevel extracts hint`() {
        val pack = parser.parsePack("classic", loadResource("classic.kye"))
        assertEquals("Collect all diamonds", pack.levels[0].hint)
    }
}

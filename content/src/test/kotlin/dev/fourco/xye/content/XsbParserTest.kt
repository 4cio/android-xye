package dev.fourco.xye.content

import dev.fourco.xye.engine.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class XsbParserTest {
    private val parser = XsbParser()

    private fun loadResource(name: String) =
        javaClass.classLoader.getResourceAsStream("testdata/$name")
            ?: throw IllegalStateException("Resource not found: testdata/$name")

    @Test
    fun `parse minimal level has correct dimensions`() {
        val level = parser.parseLevel(loadResource("minimal.xsb"), 0)
        assertEquals(5, level.width)
        assertEquals(3, level.height)
    }

    @Test
    fun `parse minimal level finds player`() {
        val level = parser.parseLevel(loadResource("minimal.xsb"), 0)
        assertEquals(Position(1, 1), level.playerStart)
    }

    @Test
    fun `parse minimal level counts gems`() {
        val level = parser.parseLevel(loadResource("minimal.xsb"), 0)
        assertEquals(1, level.totalGems)
    }

    @Test
    fun `parse minimal level has walls`() {
        val level = parser.parseLevel(loadResource("minimal.xsb"), 0)
        val walls = level.entities.filter { it.kind == EntityKind.Wall }
        // 5+5+1+1 = 12 wall tiles (top row + bottom row + two sides of middle)
        assertEquals(12, walls.size)
    }

    @Test
    fun `parse minimal level has push block`() {
        val level = parser.parseLevel(loadResource("minimal.xsb"), 0)
        val blocks = level.entities.filter { it.kind == EntityKind.PushBlock }
        assertEquals(1, blocks.size)
        assertEquals(Position(2, 1), blocks[0].pos)
    }

    @Test
    fun `toInitialState creates valid game state`() {
        val level = parser.parseLevel(loadResource("minimal.xsb"), 0)
        val state = level.toInitialState()
        assertEquals(GameStatus.Playing, state.status)
        assertEquals(0L, state.tick)
        assertNotNull(state.player())
        assertEquals(1, state.goals.totalGems)
    }

    @Test
    fun `parsePack finds multiple levels`() {
        val pack = parser.parsePack("test", loadResource("two-levels.xsb"))
        assertEquals(2, pack.levels.size)
    }

    @Test
    fun `parse level with box on goal creates both entities`() {
        // * = box on goal = PushBlock + Gem at same position
        val xsb = "####\n#@*#\n####"
        val level = parser.parseLevel(xsb.byteInputStream(), 0)
        val pos = Position(2, 1)
        val atPos = level.entities.filter { it.pos == pos }
        assertTrue(atPos.any { it.kind == EntityKind.PushBlock })
        assertTrue(atPos.any { it.kind == EntityKind.Gem })
    }

    @Test
    fun `parse level with player on goal`() {
        // + = player on goal = Player + Gem at same position
        val xsb = "#####\n#+\$.\n#####"
        val level = parser.parseLevel(xsb.byteInputStream(), 0)
        assertEquals(2, level.totalGems)
        assertNotNull(level.playerStart)
    }
}

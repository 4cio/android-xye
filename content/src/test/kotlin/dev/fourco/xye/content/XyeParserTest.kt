package dev.fourco.xye.content

import dev.fourco.xye.engine.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class XyeParserTest {
    private val parser = XyeParser()

    private fun loadResource(name: String) =
        javaClass.classLoader.getResourceAsStream("testdata/$name")
            ?: throw IllegalStateException("Resource not found: testdata/$name")

    @Test
    fun `parsePack reads pack metadata`() {
        val pack = parser.parsePack("test", loadResource("teleport.xye"))
        assertEquals("Test Pack", pack.name)
        assertEquals("Test", pack.author)
        assertEquals(2, pack.levels.size)
    }

    @Test
    fun `parsePack extracts level names`() {
        val pack = parser.parsePack("test", loadResource("teleport.xye"))
        assertEquals("Teleport Test", pack.levels[0].name)
        assertEquals("Timer Test", pack.levels[1].name)
    }

    @Test
    fun `parseLevel finds player`() {
        val level = parser.parseLevel(loadResource("teleport.xye"), 0)
        assertEquals(Position(1, 1), level.playerStart)
    }

    @Test
    fun `parseLevel finds teleporters with pair IDs`() {
        val level = parser.parseLevel(loadResource("teleport.xye"), 0)
        val teleporters = level.entities.filter { it.kind == EntityKind.Teleporter }
        assertEquals(2, teleporters.size)
        assertTrue(teleporters.all { it.props.pairId == 1 })
    }

    @Test
    fun `parseLevel finds monster`() {
        val level = parser.parseLevel(loadResource("teleport.xye"), 0)
        val monsters = level.entities.filter { it.kind == EntityKind.Monster }
        assertEquals(1, monsters.size)
        assertEquals(Position(5, 5), monsters[0].pos)
    }

    @Test
    fun `parseLevel finds timer with value`() {
        val level = parser.parseLevel(loadResource("teleport.xye"), 1)
        val timers = level.entities.filter { it.kind == EntityKind.TimerBlock }
        assertEquals(1, timers.size)
        assertEquals(3, timers[0].props.timerTicks)
    }

    @Test
    fun `parseLevel counts gems`() {
        val level = parser.parseLevel(loadResource("teleport.xye"), 0)
        assertEquals(1, level.totalGems)
    }

    @Test
    fun `toInitialState creates valid state`() {
        val level = parser.parseLevel(loadResource("teleport.xye"), 0)
        val state = level.toInitialState()
        assertEquals(GameStatus.Playing, state.status)
        assertNotNull(state.player())
    }
}

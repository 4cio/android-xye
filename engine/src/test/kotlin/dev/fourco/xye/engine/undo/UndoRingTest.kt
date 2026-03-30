package dev.fourco.xye.engine.undo

import dev.fourco.xye.engine.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class UndoRingTest {
    private fun makeState(tick: Long): GameState = GameState(
        levelId = "test",
        tick = tick,
        board = Board(10, 10),
        entities = emptyMap(),
        inventory = Inventory(),
        goals = Goals(totalGems = 0),
        status = GameStatus.Playing,
        nextEntityId = 0,
    )

    @Test
    fun `push and pop returns last state`() {
        val ring = UndoRing(10)
        val s1 = makeState(1)
        val s2 = makeState(2)
        ring.push(s1)
        ring.push(s2)
        assertEquals(s2, ring.pop())
        assertEquals(s1, ring.pop())
    }

    @Test
    fun `pop on empty returns null`() {
        val ring = UndoRing(10)
        assertNull(ring.pop())
    }

    @Test
    fun `capacity wraparound drops oldest`() {
        val ring = UndoRing(3)
        ring.push(makeState(1))
        ring.push(makeState(2))
        ring.push(makeState(3))
        ring.push(makeState(4)) // drops tick=1
        assertEquals(3, ring.currentSize())
        assertEquals(makeState(4), ring.pop())
        assertEquals(makeState(3), ring.pop())
        assertEquals(makeState(2), ring.pop())
        assertNull(ring.pop())
    }

    @Test
    fun `toList returns newest first`() {
        val ring = UndoRing(10)
        ring.push(makeState(1))
        ring.push(makeState(2))
        ring.push(makeState(3))
        val list = ring.toList()
        assertEquals(listOf(makeState(3), makeState(2), makeState(1)), list)
    }

    @Test
    fun `fromList restores state`() {
        val ring = UndoRing(10)
        val states = listOf(makeState(3), makeState(2), makeState(1))
        ring.fromList(states)
        assertEquals(makeState(3), ring.pop())
        assertEquals(makeState(2), ring.pop())
        assertEquals(makeState(1), ring.pop())
    }

    @Test
    fun `clear empties buffer`() {
        val ring = UndoRing(10)
        ring.push(makeState(1))
        ring.push(makeState(2))
        ring.clear()
        assertTrue(ring.isEmpty())
        assertNull(ring.pop())
    }
}

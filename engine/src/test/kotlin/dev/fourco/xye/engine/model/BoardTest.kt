package dev.fourco.xye.engine.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class BoardTest {
    private val board = Board(10, 10)
    private val id1 = EntityId(1)
    private val id2 = EntityId(2)
    private val pos = Position(3, 4)

    @Test
    fun `place adds entity to cell`() {
        val b = board.place(id1, pos)
        assertEquals(listOf(id1), b.entitiesAt(pos))
    }

    @Test
    fun `place multiple entities at same position`() {
        val b = board.place(id1, pos).place(id2, pos)
        assertEquals(listOf(id1, id2), b.entitiesAt(pos))
    }

    @Test
    fun `remove entity from cell`() {
        val b = board.place(id1, pos).place(id2, pos).remove(id1, pos)
        assertEquals(listOf(id2), b.entitiesAt(pos))
    }

    @Test
    fun `remove last entity cleans up cell`() {
        val b = board.place(id1, pos).remove(id1, pos)
        assertEquals(emptyList<EntityId>(), b.entitiesAt(pos))
    }

    @Test
    fun `isInBounds checks boundaries`() {
        assertTrue(board.isInBounds(Position(0, 0)))
        assertTrue(board.isInBounds(Position(9, 9)))
        assertFalse(board.isInBounds(Position(-1, 0)))
        assertFalse(board.isInBounds(Position(10, 0)))
        assertFalse(board.isInBounds(Position(0, 10)))
    }

    @Test
    fun `empty cell returns empty list`() {
        assertEquals(emptyList<EntityId>(), board.entitiesAt(Position(5, 5)))
    }
}

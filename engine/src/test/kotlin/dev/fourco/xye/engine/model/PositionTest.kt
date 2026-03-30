package dev.fourco.xye.engine.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PositionTest {
    @Test
    fun `move up decrements row`() {
        val pos = Position(5, 5)
        assertEquals(Position(5, 4), pos.move(Direction.UP))
    }

    @Test
    fun `move down increments row`() {
        val pos = Position(5, 5)
        assertEquals(Position(5, 6), pos.move(Direction.DOWN))
    }

    @Test
    fun `move left decrements col`() {
        val pos = Position(5, 5)
        assertEquals(Position(4, 5), pos.move(Direction.LEFT))
    }

    @Test
    fun `move right increments col`() {
        val pos = Position(5, 5)
        assertEquals(Position(6, 5), pos.move(Direction.RIGHT))
    }

    @Test
    fun `direction opposite`() {
        assertEquals(Direction.DOWN, Direction.UP.opposite)
        assertEquals(Direction.UP, Direction.DOWN.opposite)
        assertEquals(Direction.RIGHT, Direction.LEFT.opposite)
        assertEquals(Direction.LEFT, Direction.RIGHT.opposite)
    }
}

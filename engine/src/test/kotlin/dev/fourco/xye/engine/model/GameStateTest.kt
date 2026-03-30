package dev.fourco.xye.engine.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GameStateTest {
    private fun makeState(): GameState {
        val playerId = EntityId(0)
        val gemId = EntityId(1)
        val playerEntity = Entity(playerId, EntityKind.Player, Position(1, 1))
        val gemEntity = Entity(gemId, EntityKind.Gem, Position(3, 3))
        var board = Board(10, 10)
        board = board.place(playerId, Position(1, 1))
        board = board.place(gemId, Position(3, 3))
        return GameState(
            levelId = "test",
            tick = 0,
            board = board,
            entities = mapOf(playerId to playerEntity, gemId to gemEntity),
            inventory = Inventory(),
            goals = Goals(totalGems = 1),
            status = GameStatus.Playing,
            nextEntityId = 2,
        )
    }

    @Test
    fun `player returns player entity`() {
        val state = makeState()
        val player = state.player()
        assertNotNull(player)
        assertEquals(EntityKind.Player, player!!.kind)
    }

    @Test
    fun `moveEntity updates position and board`() {
        val state = makeState()
        val playerId = EntityId(0)
        val newPos = Position(2, 1)
        val moved = state.moveEntity(playerId, newPos)

        assertEquals(newPos, moved.entity(playerId)!!.pos)
        assertTrue(moved.board.entitiesAt(newPos).contains(playerId))
        assertFalse(moved.board.entitiesAt(Position(1, 1)).contains(playerId))
    }

    @Test
    fun `removeEntity removes from entities and board`() {
        val state = makeState()
        val gemId = EntityId(1)
        val removed = state.removeEntity(gemId)

        assertNull(removed.entity(gemId))
        assertFalse(removed.board.entitiesAt(Position(3, 3)).contains(gemId))
    }

    @Test
    fun `removeEntity with nonexistent id returns same state`() {
        val state = makeState()
        val same = state.removeEntity(EntityId(99))
        assertEquals(state, same)
    }
}

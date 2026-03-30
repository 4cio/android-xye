package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GameEngineTest {
    private fun makeLevel(): GameState {
        // Simple 5x5 level:
        // #####
        // #@.d#
        // #.B.#
        // #...#
        // #####
        val entities = mutableMapOf<EntityId, Entity>()
        var board = Board(5, 5)
        var nextId = 0

        fun add(kind: EntityKind, col: Int, row: Int, props: EntityProps = EntityProps()): EntityId {
            val id = EntityId(nextId++)
            val entity = Entity(id, kind, Position(col, row), props)
            entities[id] = entity
            board = board.place(id, Position(col, row))
            return id
        }

        // Walls (border)
        for (c in 0..4) { add(EntityKind.Wall, c, 0); add(EntityKind.Wall, c, 4) }
        for (r in 1..3) { add(EntityKind.Wall, 0, r); add(EntityKind.Wall, 4, r) }

        // Player at (1,1)
        add(EntityKind.Player, 1, 1)
        // Gem at (3,1)
        add(EntityKind.Gem, 3, 1)
        // PushBlock at (2,2)
        add(EntityKind.PushBlock, 2, 2)

        return GameState(
            levelId = "test",
            tick = 0,
            board = board,
            entities = entities,
            inventory = Inventory(),
            goals = Goals(totalGems = 1),
            status = GameStatus.Playing,
            nextEntityId = nextId,
        )
    }

    @Test
    fun `player moves right`() {
        val engine = GameEngine(makeLevel())
        engine.tick(InputIntent.MoveRight)
        val player = engine.state.player()!!
        assertEquals(Position(2, 1), player.pos)
    }

    @Test
    fun `player cannot move into wall`() {
        val engine = GameEngine(makeLevel())
        engine.tick(InputIntent.MoveUp) // wall at row 0
        val player = engine.state.player()!!
        assertEquals(Position(1, 1), player.pos) // didn't move
    }

    @Test
    fun `player pushes block`() {
        val engine = GameEngine(makeLevel())
        // Move down to (1,2), then right to push block at (2,2) to (3,2)
        engine.tick(InputIntent.MoveDown)
        engine.tick(InputIntent.MoveRight) // push block from (2,2) to (3,2)

        val player = engine.state.player()!!
        assertEquals(Position(2, 2), player.pos)

        // Find the push block — it should be at (3,2)
        val pushBlock = engine.state.entities.values.find { it.kind == EntityKind.PushBlock }!!
        assertEquals(Position(3, 2), pushBlock.pos)
    }

    @Test
    fun `collecting gem wins the game`() {
        val engine = GameEngine(makeLevel())
        engine.tick(InputIntent.MoveRight) // to (2,1)
        engine.tick(InputIntent.MoveRight) // to (3,1) - gem position
        assertEquals(GameStatus.Won, engine.state.status)
    }

    @Test
    fun `undo restores previous state`() {
        val engine = GameEngine(makeLevel())
        val before = engine.state
        engine.tick(InputIntent.MoveRight)
        assertNotEquals(before.player()!!.pos, engine.state.player()!!.pos)
        engine.tick(InputIntent.Undo)
        assertEquals(before.player()!!.pos, engine.state.player()!!.pos)
    }

    @Test
    fun `tick increments counter`() {
        val engine = GameEngine(makeLevel())
        assertEquals(0, engine.state.tick)
        engine.tick(InputIntent.Wait)
        assertEquals(1, engine.state.tick)
    }

    @Test
    fun `game ignores input after won`() {
        val engine = GameEngine(makeLevel())
        engine.tick(InputIntent.MoveRight) // (2,1)
        engine.tick(InputIntent.MoveRight) // (3,1) - win
        val wonState = engine.state
        engine.tick(InputIntent.MoveLeft) // should be ignored
        assertEquals(wonState, engine.state)
    }
}

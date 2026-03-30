package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class MagnetRuleTest {

    private val rule = MagnetRule()

    private fun buildState(init: StateBuilder.() -> Unit): GameState {
        val builder = StateBuilder()
        builder.init()
        return builder.build()
    }

    @Test
    fun `horizontal magnet pulls adjacent block toward it`() {
        // Layout (5x3):  . . M . B    (magnet at col=2, block at col=4)
        // Block at distance 2 should be pulled to distance 1 (col=3).
        val state = buildState {
            add(EntityKind.MagnetH, 2, 1)
            add(EntityKind.PushBlock, 4, 1)
        }

        val result = rule.apply(state)
        val block = result.entities.values.find { it.kind == EntityKind.PushBlock }!!
        assertEquals(Position(3, 1), block.pos)
    }

    @Test
    fun `vertical magnet pulls block below it`() {
        // Layout (3x5): magnet at (1,1), block at (1,3)
        val state = buildState {
            width = 3; height = 5
            add(EntityKind.MagnetV, 1, 1)
            add(EntityKind.PushBlock, 1, 3)
        }

        val result = rule.apply(state)
        val block = result.entities.values.find { it.kind == EntityKind.PushBlock }!!
        assertEquals(Position(1, 2), block.pos)
    }

    @Test
    fun `magnet does not affect player`() {
        val state = buildState {
            add(EntityKind.MagnetH, 2, 1)
            add(EntityKind.Player, 4, 1)
        }

        val result = rule.apply(state)
        val player = result.player()!!
        assertEquals(Position(4, 1), player.pos) // Player did not move.
    }

    @Test
    fun `magnet does not affect walls`() {
        val state = buildState {
            add(EntityKind.MagnetH, 2, 1)
            add(EntityKind.Wall, 4, 1)
        }

        val result = rule.apply(state)
        val wall = result.entities.values.find { it.kind == EntityKind.Wall }!!
        assertEquals(Position(4, 1), wall.pos)
    }

    @Test
    fun `magnet does not pull through walls`() {
        // Layout: M . W . B  -- wall blocks the magnet's scan.
        val state = buildState {
            width = 7; height = 3
            add(EntityKind.MagnetH, 1, 1)
            add(EntityKind.Wall, 2, 1)
            add(EntityKind.PushBlock, 3, 1)
        }

        val result = rule.apply(state)
        val block = result.entities.values.find { it.kind == EntityKind.PushBlock }!!
        assertEquals(Position(3, 1), block.pos) // Block did not move.
    }

    @Test
    fun `magnet does not pull block onto its own position`() {
        // Magnet at (2,1), block directly adjacent at (3,1).
        // Pull direction would put block at (2,1) == magnet position; should not move.
        val state = buildState {
            add(EntityKind.MagnetH, 2, 1)
            add(EntityKind.PushBlock, 3, 1)
        }

        val result = rule.apply(state)
        val block = result.entities.values.find { it.kind == EntityKind.PushBlock }!!
        assertEquals(Position(3, 1), block.pos) // Block stays (can't go on magnet).
    }

    @Test
    fun `magnet does not affect entities on different axis`() {
        // Horizontal magnet should not affect entity above or below.
        val state = buildState {
            add(EntityKind.MagnetH, 2, 1)
            add(EntityKind.PushBlock, 2, 3)
        }

        val result = rule.apply(state)
        val block = result.entities.values.find { it.kind == EntityKind.PushBlock }!!
        assertEquals(Position(2, 3), block.pos) // Block did not move.
    }

    /** Helper to build GameState concisely in tests. */
    private class StateBuilder {
        var width = 5
        var height = 3
        private val entities = mutableMapOf<EntityId, Entity>()
        private var board = Board(5, 3)
        private var nextId = 0

        fun add(kind: EntityKind, col: Int, row: Int, props: EntityProps = EntityProps()): EntityId {
            val id = EntityId(nextId++)
            val entity = Entity(id, kind, Position(col, row), props)
            entities[id] = entity
            board = board.place(id, Position(col, row))
            return id
        }

        fun build(): GameState {
            board = Board(width, height)
            for (entity in entities.values) {
                board = board.place(entity.id, entity.pos)
            }
            return GameState(
                levelId = "test",
                tick = 0,
                board = board,
                entities = entities,
                inventory = Inventory(),
                goals = Goals(totalGems = 0),
                status = GameStatus.Playing,
                nextEntityId = nextId,
            )
        }
    }
}

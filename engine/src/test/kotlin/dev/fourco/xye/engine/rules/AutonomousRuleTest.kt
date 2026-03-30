package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AutonomousRuleTest {

    private val rule = AutonomousRule()

    private fun buildState(init: StateBuilder.() -> Unit): GameState {
        val builder = StateBuilder()
        builder.init()
        return builder.build()
    }

    // --- Slider tests ---

    @Test
    fun `slider moves in its direction`() {
        val state = buildState {
            width = 5; height = 3
            add(EntityKind.SliderRight, 1, 1)
        }

        val result = rule.apply(state)
        val slider = result.entities.values.find { it.kind == EntityKind.SliderRight }!!
        assertEquals(Position(2, 1), slider.pos)
    }

    @Test
    fun `slider up moves up`() {
        val state = buildState {
            width = 3; height = 5
            add(EntityKind.SliderUp, 1, 2)
        }

        val result = rule.apply(state)
        val slider = result.entities.values.find { it.kind == EntityKind.SliderUp }!!
        assertEquals(Position(1, 1), slider.pos)
    }

    @Test
    fun `slider stops when blocked by wall`() {
        val state = buildState {
            width = 5; height = 3
            add(EntityKind.SliderRight, 2, 1)
            add(EntityKind.Wall, 3, 1)
        }

        val result = rule.apply(state)
        val slider = result.entities.values.find { it.kind == EntityKind.SliderRight }!!
        assertEquals(Position(2, 1), slider.pos, "Slider should not move through wall")
    }

    @Test
    fun `slider stops at board edge`() {
        val state = buildState {
            width = 5; height = 3
            add(EntityKind.SliderRight, 4, 1) // rightmost column
        }

        val result = rule.apply(state)
        val slider = result.entities.values.find { it.kind == EntityKind.SliderRight }!!
        assertEquals(Position(4, 1), slider.pos, "Slider should not move out of bounds")
    }

    @Test
    fun `slider does not change kind when blocked`() {
        val state = buildState {
            width = 5; height = 3
            add(EntityKind.SliderRight, 4, 1)
        }

        val result = rule.apply(state)
        val slider = result.entities.values.find {
            it.kind == EntityKind.SliderRight
        }
        assertNotNull(slider, "Slider should keep its kind (SliderRight)")
    }

    // --- Rocky tests ---

    @Test
    fun `rocky moves in its direction`() {
        val state = buildState {
            width = 5; height = 3
            add(EntityKind.RockyRight, 1, 1)
        }

        val result = rule.apply(state)
        val rocky = result.entities.values.find {
            it.kind == EntityKind.RockyRight || it.kind == EntityKind.RockyLeft
        }!!
        assertEquals(Position(2, 1), rocky.pos)
        assertEquals(EntityKind.RockyRight, rocky.kind, "Direction unchanged when not blocked")
    }

    @Test
    fun `rocky reverses direction when blocked`() {
        val state = buildState {
            width = 5; height = 3
            add(EntityKind.RockyRight, 3, 1)
            add(EntityKind.Wall, 4, 1)
        }

        val result = rule.apply(state)
        val rocky = result.entities.values.find {
            it.kind == EntityKind.RockyRight || it.kind == EntityKind.RockyLeft
        }!!
        assertEquals(Position(3, 1), rocky.pos, "Rocky should not have moved")
        assertEquals(EntityKind.RockyLeft, rocky.kind, "Rocky should have reversed direction")
    }

    @Test
    fun `rocky up reverses to rocky down when blocked`() {
        val state = buildState {
            width = 3; height = 5
            add(EntityKind.RockyUp, 1, 0) // top row
        }

        val result = rule.apply(state)
        val rocky = result.entities.values.find {
            it.kind in setOf(EntityKind.RockyUp, EntityKind.RockyDown,
                EntityKind.RockyLeft, EntityKind.RockyRight)
        }!!
        assertEquals(EntityKind.RockyDown, rocky.kind)
    }

    // --- Monster tests ---

    @Test
    fun `monster moves toward player horizontally`() {
        val state = buildState {
            width = 10; height = 3
            add(EntityKind.Player, 8, 1)
            add(EntityKind.Monster, 2, 1)
        }

        val result = rule.apply(state)
        val monster = result.entities.values.find { it.kind == EntityKind.Monster }!!
        assertEquals(Position(3, 1), monster.pos, "Monster should move right toward player")
    }

    @Test
    fun `monster moves toward player vertically`() {
        val state = buildState {
            width = 3; height = 10
            add(EntityKind.Player, 1, 8)
            add(EntityKind.Monster, 1, 2)
        }

        val result = rule.apply(state)
        val monster = result.entities.values.find { it.kind == EntityKind.Monster }!!
        assertEquals(Position(1, 3), monster.pos, "Monster should move down toward player")
    }

    @Test
    fun `monster moves to player cell`() {
        val state = buildState {
            width = 5; height = 3
            add(EntityKind.Player, 3, 1)
            add(EntityKind.Monster, 2, 1)
        }

        val result = rule.apply(state)
        val monster = result.entities.values.find { it.kind == EntityKind.Monster }!!
        assertEquals(Position(3, 1), monster.pos, "Monster should move onto player's cell")
    }

    @Test
    fun `monster stays put when surrounded by walls`() {
        val state = buildState {
            width = 5; height = 5
            add(EntityKind.Player, 4, 4)
            add(EntityKind.Monster, 2, 2)
            add(EntityKind.Wall, 3, 2) // right
            add(EntityKind.Wall, 1, 2) // left
            add(EntityKind.Wall, 2, 1) // up
            add(EntityKind.Wall, 2, 3) // down
        }

        val result = rule.apply(state)
        val monster = result.entities.values.find { it.kind == EntityKind.Monster }!!
        assertEquals(Position(2, 2), monster.pos, "Monster should stay put when blocked")
    }

    @Test
    fun `monster tries alternate direction when primary is blocked`() {
        // Player is to the right and below.
        // Wall blocks rightward movement, so monster should move down.
        val state = buildState {
            width = 10; height = 10
            add(EntityKind.Player, 8, 8)
            add(EntityKind.Monster, 2, 2)
            add(EntityKind.Wall, 3, 2) // blocks right
        }

        val result = rule.apply(state)
        val monster = result.entities.values.find { it.kind == EntityKind.Monster }!!
        // Primary dir is RIGHT (dx=6), secondary is DOWN (dy=6). dx==dy so it tries
        // horizontal first. Since right is blocked, it should try DOWN.
        assertEquals(Position(2, 3), monster.pos,
            "Monster should move down when horizontal is blocked")
    }

    private class StateBuilder {
        var width = 5
        var height = 5
        private val entities = mutableMapOf<EntityId, Entity>()
        private var nextId = 0

        fun add(kind: EntityKind, col: Int, row: Int, props: EntityProps = EntityProps()): EntityId {
            val id = EntityId(nextId++)
            entities[id] = Entity(id, kind, Position(col, row), props)
            return id
        }

        fun build(): GameState {
            var board = Board(width, height)
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

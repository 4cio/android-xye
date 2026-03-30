package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TriggerRuleTest {

    private val rule = TriggerRule()

    private fun buildState(init: StateBuilder.() -> Unit): GameState {
        val builder = StateBuilder()
        builder.init()
        return builder.build()
    }

    @Test
    fun `player on trigger removes matching doors`() {
        val state = buildState {
            add(EntityKind.Player, 2, 1)
            add(EntityKind.Trigger, 2, 1, EntityProps(triggerId = 1))
            add(EntityKind.Door, 4, 1, EntityProps(triggerId = 1))
            add(EntityKind.Door, 4, 2, EntityProps(triggerId = 1))
        }

        val result = rule.apply(state)
        val doors = result.entities.values.filter { it.kind == EntityKind.Door }
        assertEquals(0, doors.size, "Both doors with triggerId=1 should be removed")
    }

    @Test
    fun `trigger does not remove doors with different triggerId`() {
        val state = buildState {
            add(EntityKind.Player, 2, 1)
            add(EntityKind.Trigger, 2, 1, EntityProps(triggerId = 1))
            add(EntityKind.Door, 4, 1, EntityProps(triggerId = 2))
        }

        val result = rule.apply(state)
        val doors = result.entities.values.filter { it.kind == EntityKind.Door }
        assertEquals(1, doors.size, "Door with triggerId=2 should remain")
    }

    @Test
    fun `no effect when player is not on trigger`() {
        val state = buildState {
            add(EntityKind.Player, 1, 1)
            add(EntityKind.Trigger, 3, 1, EntityProps(triggerId = 1))
            add(EntityKind.Door, 4, 1, EntityProps(triggerId = 1))
        }

        val result = rule.apply(state)
        val doors = result.entities.values.filter { it.kind == EntityKind.Door }
        assertEquals(1, doors.size, "Door should remain since player is not on trigger")
    }

    @Test
    fun `no effect when no player exists`() {
        val state = buildState {
            add(EntityKind.Trigger, 2, 1, EntityProps(triggerId = 1))
            add(EntityKind.Door, 4, 1, EntityProps(triggerId = 1))
        }

        val result = rule.apply(state)
        val doors = result.entities.values.filter { it.kind == EntityKind.Door }
        assertEquals(1, doors.size)
    }

    private class StateBuilder {
        var width = 5
        var height = 3
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

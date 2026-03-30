package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TimerRuleTest {

    private val rule = TimerRule()

    private fun buildState(init: StateBuilder.() -> Unit): GameState {
        val builder = StateBuilder()
        builder.init()
        return builder.build()
    }

    @Test
    fun `timer decrements each tick`() {
        val state = buildState {
            add(EntityKind.TimerBlock, 2, 1, EntityProps(timerTicks = 5))
        }

        val result = rule.apply(state)
        val timer = result.entities.values.find { it.kind == EntityKind.TimerBlock }!!
        assertEquals(4, timer.props.timerTicks)
    }

    @Test
    fun `timer vanishes at zero`() {
        val state = buildState {
            add(EntityKind.TimerBlock, 2, 1, EntityProps(timerTicks = 1))
        }

        val result = rule.apply(state)
        val timer = result.entities.values.find { it.kind == EntityKind.TimerBlock }
        assertNull(timer, "Timer should be removed when it reaches 0")
    }

    @Test
    fun `timer with ticks 2 survives one application`() {
        val state = buildState {
            add(EntityKind.TimerBlock, 2, 1, EntityProps(timerTicks = 2))
        }

        val result = rule.apply(state)
        val timer = result.entities.values.find { it.kind == EntityKind.TimerBlock }
        assertNotNull(timer, "Timer with 2 ticks should survive one tick")
        assertEquals(1, timer!!.props.timerTicks)
    }

    @Test
    fun `multiple timers each decrement independently`() {
        val state = buildState {
            add(EntityKind.TimerBlock, 1, 1, EntityProps(timerTicks = 3))
            add(EntityKind.TimerBlock, 3, 1, EntityProps(timerTicks = 1))
        }

        val result = rule.apply(state)
        val timers = result.entities.values.filter { it.kind == EntityKind.TimerBlock }
        assertEquals(1, timers.size, "Second timer should be gone")
        assertEquals(2, timers.first().props.timerTicks)
    }

    @Test
    fun `timer without timerTicks prop is unaffected`() {
        val state = buildState {
            add(EntityKind.TimerBlock, 2, 1) // no timerTicks set
        }

        val result = rule.apply(state)
        val timer = result.entities.values.find { it.kind == EntityKind.TimerBlock }
        assertNotNull(timer, "Timer without timerTicks should remain")
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

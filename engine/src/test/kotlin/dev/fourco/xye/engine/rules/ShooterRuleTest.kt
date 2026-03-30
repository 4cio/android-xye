package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ShooterRuleTest {

    private val rule = ShooterRule()

    private fun buildState(
        tick: Long = 0,
        init: StateBuilder.() -> Unit,
    ): GameState {
        val builder = StateBuilder(tick)
        builder.init()
        return builder.build()
    }

    @Test
    fun `shooter spawns entity at correct interval`() {
        val state = buildState(tick = 0) {
            add(EntityKind.Shooter, 2, 1, EntityProps(
                direction = Direction.RIGHT,
                shootInterval = 10,
                shootKind = EntityKind.SliderRight,
            ))
        }

        val result = rule.apply(state)

        // tick=0, 0 % 10 == 0 => should spawn
        val sliders = result.entities.values.filter { it.kind == EntityKind.SliderRight }
        assertEquals(1, sliders.size, "Shooter should spawn a slider on tick 0")
        assertEquals(Position(3, 1), sliders.first().pos, "Slider should be in front of shooter")
    }

    @Test
    fun `shooter does not spawn on non-interval ticks`() {
        val state = buildState(tick = 3) {
            add(EntityKind.Shooter, 2, 1, EntityProps(
                direction = Direction.RIGHT,
                shootInterval = 10,
                shootKind = EntityKind.SliderRight,
            ))
        }

        val result = rule.apply(state)
        val sliders = result.entities.values.filter { it.kind == EntityKind.SliderRight }
        assertEquals(0, sliders.size, "Shooter should not spawn on tick 3 (interval 10)")
    }

    @Test
    fun `shooter spawns at exact interval`() {
        val state = buildState(tick = 10) {
            add(EntityKind.Shooter, 2, 1, EntityProps(
                direction = Direction.RIGHT,
                shootInterval = 10,
                shootKind = EntityKind.SliderRight,
            ))
        }

        val result = rule.apply(state)
        val sliders = result.entities.values.filter { it.kind == EntityKind.SliderRight }
        assertEquals(1, sliders.size, "Shooter should spawn on tick 10")
    }

    @Test
    fun `shooter does not spawn into occupied cell`() {
        val state = buildState(tick = 0) {
            add(EntityKind.Shooter, 2, 1, EntityProps(
                direction = Direction.RIGHT,
                shootInterval = 10,
                shootKind = EntityKind.SliderRight,
            ))
            add(EntityKind.Wall, 3, 1) // blocking the spawn position
        }

        val result = rule.apply(state)
        val sliders = result.entities.values.filter { it.kind == EntityKind.SliderRight }
        assertEquals(0, sliders.size, "Shooter should not spawn into a wall")
    }

    @Test
    fun `shooter increments nextEntityId`() {
        val state = buildState(tick = 0) {
            add(EntityKind.Shooter, 2, 1, EntityProps(
                direction = Direction.DOWN,
                shootInterval = 5,
                shootKind = EntityKind.SliderDown,
            ))
        }

        val initialNextId = state.nextEntityId
        val result = rule.apply(state)
        assertEquals(initialNextId + 1, result.nextEntityId,
            "nextEntityId should be incremented after spawning")
    }

    @Test
    fun `shooter does not spawn out of bounds`() {
        val state = buildState(tick = 0) {
            width = 5; height = 3
            // Shooter at right edge, shooting right (out of bounds)
            add(EntityKind.Shooter, 4, 1, EntityProps(
                direction = Direction.RIGHT,
                shootInterval = 5,
                shootKind = EntityKind.SliderRight,
            ))
        }

        val result = rule.apply(state)
        val sliders = result.entities.values.filter { it.kind == EntityKind.SliderRight }
        assertEquals(0, sliders.size, "Should not spawn out of bounds")
    }

    @Test
    fun `shooter without direction does nothing`() {
        val state = buildState(tick = 0) {
            add(EntityKind.Shooter, 2, 1, EntityProps(
                shootInterval = 5,
                shootKind = EntityKind.SliderRight,
                // no direction set
            ))
        }

        val result = rule.apply(state)
        assertEquals(state.entities.size, result.entities.size)
    }

    private class StateBuilder(private val tick: Long) {
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
                tick = tick,
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

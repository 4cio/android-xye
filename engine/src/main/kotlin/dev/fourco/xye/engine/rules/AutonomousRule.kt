package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*
import kotlin.math.abs

/**
 * Handles all self-moving entities:
 *
 * **Sliders** move one cell in their fixed direction each tick.
 * If blocked, they stay put (become inert but keep their kind).
 *
 * **Rockies** move one cell in their direction each tick.
 * If blocked, they reverse direction (EntityKind changes to the opposite).
 *
 * **Monsters** chase the player using simple Manhattan distance reduction.
 * They try horizontal movement first, then vertical. If neither works,
 * they stay put. Monsters can only move to empty cells or cells occupied
 * by the player (to trigger a collision).
 */
class AutonomousRule : GameRule {

    companion object {
        private val SLIDER_KINDS = setOf(
            EntityKind.SliderUp, EntityKind.SliderDown,
            EntityKind.SliderLeft, EntityKind.SliderRight,
        )
        private val ROCKY_KINDS = setOf(
            EntityKind.RockyUp, EntityKind.RockyDown,
            EntityKind.RockyLeft, EntityKind.RockyRight,
        )
        private val BLOCKING_KINDS = setOf(
            EntityKind.Wall, EntityKind.PushBlock, EntityKind.RoundBlock,
            EntityKind.Door, EntityKind.BlackHole, EntityKind.MagnetH,
            EntityKind.MagnetV,
        )
    }

    override fun apply(state: GameState): GameState {
        var next = state

        // Process sliders.
        val sliders = next.entities.values.filter { it.kind in SLIDER_KINDS }
        for (slider in sliders) {
            val dir = sliderDirection(slider.kind) ?: continue
            val target = slider.pos.move(dir)
            if (canMoveToForSlider(next, target)) {
                next = next.moveEntity(slider.id, target)
            }
            // If blocked, slider stays put (no direction change).
        }

        // Process rockies.
        val rockies = next.entities.values.filter { it.kind in ROCKY_KINDS }
        for (rocky in rockies) {
            val dir = rockyDirection(rocky.kind) ?: continue
            val target = rocky.pos.move(dir)
            if (canMoveToForSlider(next, target)) {
                next = next.moveEntity(rocky.id, target)
            } else {
                // Reverse direction.
                val reversedKind = reverseRockyKind(rocky.kind)
                val updated = rocky.copy(kind = reversedKind)
                next = next.updateEntity(updated)
            }
        }

        // Process monsters.
        val player = next.player()
        if (player != null) {
            val monsters = next.entities.values.filter { it.kind == EntityKind.Monster }
            for (monster in monsters) {
                next = moveMonster(next, monster, player.pos)
            }
        }

        return next
    }

    private fun moveMonster(state: GameState, monster: Entity, playerPos: Position): GameState {
        val dx = playerPos.col - monster.pos.col
        val dy = playerPos.row - monster.pos.row

        // Determine preferred horizontal and vertical directions.
        val horizDir = if (dx > 0) Direction.RIGHT else if (dx < 0) Direction.LEFT else null
        val vertDir = if (dy > 0) Direction.DOWN else if (dy < 0) Direction.UP else null

        // Try horizontal first (if the horizontal distance is >= vertical distance),
        // otherwise try vertical first.
        val firstDir: Direction?
        val secondDir: Direction?
        if (abs(dx) >= abs(dy)) {
            firstDir = horizDir
            secondDir = vertDir
        } else {
            firstDir = vertDir
            secondDir = horizDir
        }

        // Try first preferred direction.
        if (firstDir != null) {
            val target = monster.pos.move(firstDir)
            if (canMoveToForMonster(state, target)) {
                return state.moveEntity(monster.id, target)
            }
        }

        // Try second preferred direction.
        if (secondDir != null) {
            val target = monster.pos.move(secondDir)
            if (canMoveToForMonster(state, target)) {
                return state.moveEntity(monster.id, target)
            }
        }

        // Can't move — stay put.
        return state
    }

    /** Monsters can move to empty cells or cells with the player. */
    private fun canMoveToForMonster(state: GameState, pos: Position): Boolean {
        if (!state.board.isInBounds(pos)) return false
        val entities = state.board.entitiesAt(pos).mapNotNull { state.entity(it) }
        return entities.none { it.kind in BLOCKING_KINDS && it.kind != EntityKind.Player }
    }

    /** Sliders and rockies can move to empty, non-blocking cells. */
    private fun canMoveToForSlider(state: GameState, pos: Position): Boolean {
        if (!state.board.isInBounds(pos)) return false
        val entities = state.board.entitiesAt(pos).mapNotNull { state.entity(it) }
        return entities.none { it.kind in BLOCKING_KINDS || it.kind == EntityKind.Player }
    }

    private fun sliderDirection(kind: EntityKind): Direction? = when (kind) {
        EntityKind.SliderUp -> Direction.UP
        EntityKind.SliderDown -> Direction.DOWN
        EntityKind.SliderLeft -> Direction.LEFT
        EntityKind.SliderRight -> Direction.RIGHT
        else -> null
    }

    private fun rockyDirection(kind: EntityKind): Direction? = when (kind) {
        EntityKind.RockyUp -> Direction.UP
        EntityKind.RockyDown -> Direction.DOWN
        EntityKind.RockyLeft -> Direction.LEFT
        EntityKind.RockyRight -> Direction.RIGHT
        else -> null
    }

    private fun reverseRockyKind(kind: EntityKind): EntityKind = when (kind) {
        EntityKind.RockyUp -> EntityKind.RockyDown
        EntityKind.RockyDown -> EntityKind.RockyUp
        EntityKind.RockyLeft -> EntityKind.RockyRight
        EntityKind.RockyRight -> EntityKind.RockyLeft
        else -> kind
    }
}

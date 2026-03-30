package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*

/**
 * Shooters periodically spawn new entities in front of them.
 *
 * Each Shooter has:
 * - [EntityProps.shootInterval] (default 10): spawn occurs when `tick % interval == 0`
 * - [EntityProps.shootKind] (default SliderUp): the kind of entity to create
 * - [EntityProps.direction]: the direction the shooter faces; spawn occurs in that cell
 *
 * A spawn only happens if the target cell is empty (no blocking entities).
 * New entity IDs are allocated from [GameState.nextEntityId].
 */
class ShooterRule : GameRule {

    companion object {
        private const val DEFAULT_INTERVAL = 10
        private val DEFAULT_SHOOT_KIND = EntityKind.SliderUp

        private val BLOCKING_KINDS = setOf(
            EntityKind.Wall, EntityKind.PushBlock, EntityKind.RoundBlock,
            EntityKind.Door, EntityKind.BlackHole, EntityKind.MagnetH,
            EntityKind.MagnetV, EntityKind.Player,
        )
    }

    override fun apply(state: GameState): GameState {
        val shooters = state.entities.values.filter { it.kind == EntityKind.Shooter }
        if (shooters.isEmpty()) return state

        var next = state

        for (shooter in shooters) {
            val interval = shooter.props.shootInterval ?: DEFAULT_INTERVAL
            if (interval <= 0) continue
            if (next.tick % interval != 0L) continue

            val dir = shooter.props.direction ?: continue
            val spawnPos = shooter.pos.move(dir)
            if (!next.board.isInBounds(spawnPos)) continue

            // Check if the spawn position is empty.
            val entitiesAtSpawn = next.board.entitiesAt(spawnPos)
                .mapNotNull { next.entity(it) }
            if (entitiesAtSpawn.any { it.kind in BLOCKING_KINDS }) continue

            val newId = EntityId(next.nextEntityId)
            val spawnKind = shooter.props.shootKind ?: DEFAULT_SHOOT_KIND
            val newEntity = Entity(
                id = newId,
                kind = spawnKind,
                pos = spawnPos,
            )

            next = next.copy(
                entities = next.entities + (newId to newEntity),
                board = next.board.place(newId, spawnPos),
                nextEntityId = next.nextEntityId + 1,
            )
        }

        return next
    }
}

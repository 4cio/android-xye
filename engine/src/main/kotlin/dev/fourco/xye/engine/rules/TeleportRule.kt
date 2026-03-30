package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*

/**
 * Teleporters come in pairs identified by [EntityProps.pairId].
 * When any entity (including the player) shares a position with a
 * teleporter, it is transported to the paired teleporter's position.
 *
 * Entities that are already sitting on their destination teleporter
 * are not re-teleported, preventing infinite loops.
 */
class TeleportRule : GameRule {

    override fun apply(state: GameState): GameState {
        // Build a lookup: pairId -> list of teleporter entities.
        val teleportersByPairId = state.entities.values
            .filter { it.kind == EntityKind.Teleporter && it.props.pairId != null }
            .groupBy { it.props.pairId!! }

        if (teleportersByPairId.isEmpty()) return state

        var next = state

        // Track which entities have already been teleported this tick to prevent
        // re-teleporting in the same pass.
        val teleportedThisTick = mutableSetOf<EntityId>()

        for ((_, teleporters) in teleportersByPairId) {
            if (teleporters.size < 2) continue

            for (teleporter in teleporters) {
                // Find the paired teleporter (same pairId, different position).
                val paired = teleporters.find { it.id != teleporter.id } ?: continue

                // Find all non-teleporter entities at this teleporter's position.
                val entitiesOnTeleporter = next.board.entitiesAt(teleporter.pos)
                    .mapNotNull { next.entity(it) }
                    .filter { it.kind != EntityKind.Teleporter }

                for (entity in entitiesOnTeleporter) {
                    if (entity.id in teleportedThisTick) continue

                    // Don't teleport if the entity is already at the destination.
                    if (entity.pos == paired.pos) continue

                    next = next.moveEntity(entity.id, paired.pos)
                    teleportedThisTick.add(entity.id)
                }
            }
        }

        return next
    }
}

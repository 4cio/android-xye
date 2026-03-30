package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*

class CollisionRule : GameRule {
    override fun apply(state: GameState): GameState {
        val player = state.player() ?: return state
        var next = state

        // Check if player is on a black hole, hazard, or monster
        val atPlayer = next.board.entitiesAt(player.pos)
            .mapNotNull { next.entity(it) }
            .filter { it.id != player.id }

        for (entity in atPlayer) {
            when (entity.kind) {
                EntityKind.BlackHole, EntityKind.Hazard, EntityKind.Monster -> {
                    next = next.removeEntity(player.id)
                }
                else -> {}
            }
        }

        return next
    }
}

package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*

/**
 * When the player stands on a Trigger, all Door entities with the
 * matching [EntityProps.triggerId] are removed from the game state.
 */
class TriggerRule : GameRule {

    override fun apply(state: GameState): GameState {
        val player = state.player() ?: return state

        // Find all triggers the player is standing on.
        val activatedTriggerIds = state.board.entitiesAt(player.pos)
            .mapNotNull { state.entity(it) }
            .filter { it.kind == EntityKind.Trigger }
            .mapNotNull { it.props.triggerId }
            .toSet()

        if (activatedTriggerIds.isEmpty()) return state

        // Find all doors whose triggerId matches any activated trigger.
        val doorsToRemove = state.entities.values.filter {
            it.kind == EntityKind.Door && it.props.triggerId in activatedTriggerIds
        }

        if (doorsToRemove.isEmpty()) return state

        var next = state
        for (door in doorsToRemove) {
            next = next.removeEntity(door.id)
        }
        return next
    }
}

package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*

class CollectionRule : GameRule {
    override fun apply(state: GameState): GameState {
        val player = state.player() ?: return state
        val atPlayer = state.board.entitiesAt(player.pos)
            .mapNotNull { state.entity(it) }
            .filter { it.id != player.id }

        var next = state
        for (entity in atPlayer) {
            when (entity.kind) {
                EntityKind.Gem -> {
                    next = next.removeEntity(entity.id)
                    next = next.copy(goals = next.goals.copy(
                        collectedGems = next.goals.collectedGems + 1
                    ))
                }
                EntityKind.StarGem -> {
                    next = next.removeEntity(entity.id)
                    next = next.copy(
                        goals = next.goals.copy(
                            collectedStars = next.goals.collectedStars + 1
                        ),
                        inventory = next.inventory.copy(
                            starGems = next.inventory.starGems + 1
                        ),
                    )
                }
                EntityKind.Key -> {
                    next = next.removeEntity(entity.id)
                    val color = entity.props.color ?: 0
                    val count = next.inventory.keys.getOrDefault(color, 0)
                    next = next.copy(
                        inventory = next.inventory.copy(
                            keys = next.inventory.keys + (color to count + 1)
                        )
                    )
                }
                EntityKind.SoftBlock -> {
                    next = next.removeEntity(entity.id)
                }
                else -> {}
            }
        }
        return next
    }
}

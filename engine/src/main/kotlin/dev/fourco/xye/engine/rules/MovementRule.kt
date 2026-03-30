package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*

class MovementRule : GameRule {
    override fun apply(state: GameState): GameState {
        val player = state.player() ?: return state
        val input = state.pendingInput ?: return state
        val dir = inputToDirection(input) ?: return state
        val target = player.pos.move(dir)

        if (!state.board.isInBounds(target)) return state

        // Check what's at the target
        val targetEntities = state.board.entitiesAt(target)
            .mapNotNull { state.entity(it) }

        // Wall blocks movement
        if (targetEntities.any { it.kind == EntityKind.Wall }) return state

        // Try to push pushable blocks
        val pushable = targetEntities.find {
            it.kind == EntityKind.PushBlock || it.kind == EntityKind.RoundBlock
        }

        var nextState = state
        if (pushable != null) {
            val pushTarget = pushable.pos.move(dir)
            if (!canMoveTo(nextState, pushTarget)) return state
            nextState = nextState.moveEntity(pushable.id, pushTarget)
        } else if (targetEntities.any { isBlocking(it.kind) }) {
            return state
        }

        // Move player
        nextState = nextState.moveEntity(player.id, target)
        return nextState
    }

    private fun canMoveTo(state: GameState, pos: Position): Boolean {
        if (!state.board.isInBounds(pos)) return false
        val entities = state.board.entitiesAt(pos).mapNotNull { state.entity(it) }
        return entities.none { isBlocking(it.kind) }
    }

    private fun isBlocking(kind: EntityKind): Boolean = when (kind) {
        EntityKind.Wall, EntityKind.PushBlock, EntityKind.RoundBlock,
        EntityKind.Door, EntityKind.BlackHole -> true
        else -> false
    }

    private fun inputToDirection(input: InputIntent): Direction? = when (input) {
        is InputIntent.MoveUp -> Direction.UP
        is InputIntent.MoveDown -> Direction.DOWN
        is InputIntent.MoveLeft -> Direction.LEFT
        is InputIntent.MoveRight -> Direction.RIGHT
        else -> null
    }
}

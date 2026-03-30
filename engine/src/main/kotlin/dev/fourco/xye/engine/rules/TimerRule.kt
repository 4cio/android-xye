package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*

/**
 * Timer blocks count down and vanish.
 *
 * Each [EntityKind.TimerBlock] has [EntityProps.timerTicks].
 * Every tick the value is decremented by 1.
 * When it reaches 0, the entity is removed from the game state.
 */
class TimerRule : GameRule {

    override fun apply(state: GameState): GameState {
        val timers = state.entities.values.filter { it.kind == EntityKind.TimerBlock }
        if (timers.isEmpty()) return state

        var next = state

        for (timer in timers) {
            val remaining = timer.props.timerTicks ?: continue
            val newRemaining = remaining - 1

            if (newRemaining <= 0) {
                next = next.removeEntity(timer.id)
            } else {
                val updated = timer.copy(
                    props = timer.props.copy(timerTicks = newRemaining)
                )
                next = next.updateEntity(updated)
            }
        }

        return next
    }
}

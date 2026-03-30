package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*

class WinLossRule : GameRule {
    override fun apply(state: GameState): GameState {
        if (state.status != GameStatus.Playing) return state

        // Check if player exists (might have been destroyed)
        if (state.player() == null) {
            return state.copy(status = GameStatus.Lost)
        }

        // Check win: all gems collected
        if (state.goals.allGemsCollected) {
            // Stars must be collected before last gem (if any exist)
            if (state.goals.starsRequired && !state.goals.allStarsCollected) {
                // Stars not collected — game continues (but no more gems to collect, so effectively lost)
                return state.copy(status = GameStatus.Lost)
            }
            return state.copy(status = GameStatus.Won)
        }

        return state
    }
}

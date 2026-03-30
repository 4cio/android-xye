package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*

class WinLossRule : GameRule {
    override fun apply(state: GameState): GameState {
        if (state.status != GameStatus.Playing) return state

        // Check if player exists (might have been destroyed)
        if (state.player() == null) {
            return state.copy(status = GameStatus.Lost)
        }

        // Need at least one collectible to have a win condition
        if (state.goals.totalGems == 0 && !state.goals.starsRequired) {
            return state
        }

        // Check win: all gems collected (and stars if required)
        if (state.goals.allGemsCollected) {
            if (state.goals.starsRequired && !state.goals.allStarsCollected) {
                // Stars not collected but all gems gone — lost
                return state.copy(status = GameStatus.Lost)
            }
            return state.copy(status = GameStatus.Won)
        }

        return state
    }
}

package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.GameState

interface GameRule {
    fun apply(state: GameState): GameState
}

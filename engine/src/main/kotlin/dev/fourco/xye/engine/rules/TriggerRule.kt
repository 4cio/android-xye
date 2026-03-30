package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.GameState

class TriggerRule : GameRule {
    override fun apply(state: GameState) = state
}

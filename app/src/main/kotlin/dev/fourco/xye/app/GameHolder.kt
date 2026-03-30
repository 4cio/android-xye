package dev.fourco.xye.app

import dev.fourco.xye.app.viewmodel.GameViewModel
import dev.fourco.xye.content.LevelPack

/**
 * Retained game state that survives configuration changes.
 * Held at the Application level since we're not using a DI framework.
 */
object GameHolder {
    var currentViewModel: GameViewModel? = null
    var packs: List<LevelPack> = emptyList()
    var selectedPack: LevelPack? = null
    var selectedLevelIndex: Int = 0
    var currentScreen: String = "home"
}

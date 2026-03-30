package dev.fourco.xye.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class Inventory(
    val keys: Map<Int, Int> = emptyMap(),
    val starGems: Int = 0,
)

@Serializable
data class Goals(
    val totalGems: Int,
    val collectedGems: Int = 0,
    val totalStars: Int = 0,
    val collectedStars: Int = 0,
) {
    val allGemsCollected: Boolean get() = collectedGems >= totalGems
    val allStarsCollected: Boolean get() = collectedStars >= totalStars
    val starsRequired: Boolean get() = totalStars > 0
}

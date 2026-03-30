package dev.fourco.xye.content

import kotlinx.serialization.Serializable

@Serializable
data class LevelPack(
    val id: String,
    val name: String,
    val author: String = "",
    val description: String = "",
    val format: LevelFormat,
    val levels: List<LevelMeta>,
)

@Serializable
enum class LevelFormat { XYE, KYE, XSB }

@Serializable
data class LevelMeta(
    val index: Int,
    val id: String,
    val name: String,
    val hint: String = "",
    val width: Int,
    val height: Int,
)

package dev.fourco.xye.engine.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface InputIntent {
    @Serializable data object MoveUp : InputIntent
    @Serializable data object MoveDown : InputIntent
    @Serializable data object MoveLeft : InputIntent
    @Serializable data object MoveRight : InputIntent
    @Serializable data object Undo : InputIntent
    @Serializable data object Wait : InputIntent
}

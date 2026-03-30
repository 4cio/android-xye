package dev.fourco.xye.app.ui.input

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import dev.fourco.xye.engine.model.InputIntent
import kotlin.math.abs

fun Modifier.swipeInput(
    onInput: (InputIntent) -> Unit,
    threshold: Float = 40f,
): Modifier = this.pointerInput(Unit) {
    var cumDx = 0f
    var cumDy = 0f

    detectDragGestures(
        onDragStart = { cumDx = 0f; cumDy = 0f },
        onDrag = { _, dragAmount ->
            cumDx += dragAmount.x
            cumDy += dragAmount.y

            if (abs(cumDx) >= threshold || abs(cumDy) >= threshold) {
                val intent = if (abs(cumDx) > abs(cumDy)) {
                    if (cumDx > 0) InputIntent.MoveRight else InputIntent.MoveLeft
                } else {
                    if (cumDy > 0) InputIntent.MoveDown else InputIntent.MoveUp
                }
                onInput(intent)
                cumDx = 0f
                cumDy = 0f
            }
        },
    )
}

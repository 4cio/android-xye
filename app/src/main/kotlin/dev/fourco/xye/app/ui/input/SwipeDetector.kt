package dev.fourco.xye.app.ui.input

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import dev.fourco.xye.engine.model.InputIntent
import kotlin.math.abs

fun Modifier.swipeInput(
    onInput: (InputIntent) -> Unit,
    threshold: Float = 50f,
): Modifier = this.pointerInput(Unit) {
    detectDragGestures { _, dragAmount ->
        val (dx, dy) = dragAmount
        if (abs(dx) < threshold && abs(dy) < threshold) return@detectDragGestures

        val intent = if (abs(dx) > abs(dy)) {
            if (dx > 0) InputIntent.MoveRight else InputIntent.MoveLeft
        } else {
            if (dy > 0) InputIntent.MoveDown else InputIntent.MoveUp
        }
        onInput(intent)
    }
}

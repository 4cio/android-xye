package dev.fourco.xye.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.fourco.xye.engine.model.*

@Composable
fun BoardCanvas(
    state: GameState,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cellSize = minOf(
            size.width / state.board.width,
            size.height / state.board.height,
        )
        val offsetX = (size.width - cellSize * state.board.width) / 2
        val offsetY = (size.height - cellSize * state.board.height) / 2

        // Draw grid background
        for (row in 0 until state.board.height) {
            for (col in 0 until state.board.width) {
                val x = offsetX + col * cellSize
                val y = offsetY + row * cellSize
                drawRect(
                    color = Color(0xFF1A1A2E),
                    topLeft = Offset(x, y),
                    size = Size(cellSize, cellSize),
                )
                // Grid lines
                drawRect(
                    color = Color(0xFF16213E),
                    topLeft = Offset(x, y),
                    size = Size(cellSize, cellSize),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5f),
                )
            }
        }

        // Draw entities
        for (entity in state.entities.values) {
            val x = offsetX + entity.pos.col * cellSize
            val y = offsetY + entity.pos.row * cellSize
            drawEntity(entity, x, y, cellSize)
        }
    }
}

private fun DrawScope.drawEntity(entity: Entity, x: Float, y: Float, cellSize: Float) {
    val padding = cellSize * 0.05f
    val innerSize = cellSize - padding * 2

    when (entity.kind) {
        EntityKind.Player -> {
            drawCircle(
                color = Color(0xFF00E676),
                radius = innerSize / 2,
                center = Offset(x + cellSize / 2, y + cellSize / 2),
            )
        }
        EntityKind.Wall -> {
            drawRect(
                color = Color(0xFF424242),
                topLeft = Offset(x + padding, y + padding),
                size = Size(innerSize, innerSize),
            )
        }
        EntityKind.Gem -> {
            // Diamond shape
            val cx = x + cellSize / 2
            val cy = y + cellSize / 2
            val r = innerSize / 2
            val path = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r, cy)
                lineTo(cx, cy + r)
                lineTo(cx - r, cy)
                close()
            }
            drawPath(path, color = Color(0xFF00BCD4))
        }
        EntityKind.StarGem -> {
            drawCircle(
                color = Color(0xFFFFD700),
                radius = innerSize / 2,
                center = Offset(x + cellSize / 2, y + cellSize / 2),
            )
        }
        EntityKind.PushBlock -> {
            drawRect(
                color = Color(0xFF8D6E63),
                topLeft = Offset(x + padding, y + padding),
                size = Size(innerSize, innerSize),
            )
        }
        EntityKind.RoundBlock -> {
            drawCircle(
                color = Color(0xFF8D6E63),
                radius = innerSize / 2,
                center = Offset(x + cellSize / 2, y + cellSize / 2),
            )
        }
        EntityKind.BlackHole -> {
            drawCircle(
                color = Color.Black,
                radius = innerSize / 2,
                center = Offset(x + cellSize / 2, y + cellSize / 2),
            )
        }
        EntityKind.Monster -> {
            drawCircle(
                color = Color(0xFFFF1744),
                radius = innerSize / 2,
                center = Offset(x + cellSize / 2, y + cellSize / 2),
            )
        }
        EntityKind.Hazard -> {
            drawRect(
                color = Color(0xFFFF1744),
                topLeft = Offset(x + padding, y + padding),
                size = Size(innerSize, innerSize),
            )
        }
        EntityKind.Door -> {
            drawRect(
                color = Color(0xFFFF9800),
                topLeft = Offset(x + padding, y + padding),
                size = Size(innerSize, innerSize),
            )
        }
        EntityKind.Key -> {
            drawCircle(
                color = Color(0xFFFF9800),
                radius = innerSize / 3,
                center = Offset(x + cellSize / 2, y + cellSize / 2),
            )
        }
        EntityKind.SoftBlock -> {
            drawRect(
                color = Color(0xFF9E9E9E),
                topLeft = Offset(x + padding, y + padding),
                size = Size(innerSize, innerSize),
            )
        }
        EntityKind.Teleporter -> {
            val cx = x + cellSize / 2
            val cy = y + cellSize / 2
            val r = innerSize / 2
            val path = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r, cy)
                lineTo(cx, cy + r)
                lineTo(cx - r, cy)
                close()
            }
            drawPath(path, color = Color(0xFFAB47BC))
        }
        else -> {
            // Generic fallback: small gray square
            drawRect(
                color = Color(0xFF607D8B),
                topLeft = Offset(x + padding * 2, y + padding * 2),
                size = Size(innerSize - padding * 2, innerSize - padding * 2),
            )
        }
    }
}

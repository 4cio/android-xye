package dev.fourco.xye.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.fourco.xye.engine.model.*

// Level wall tint (sandy tan like the original Xye)
private val WallBase = Color(0xFFC4B090)
private val WallDark = Color(0xFFA08860)
private val WallLight = Color(0xFFDDD0B8)
private val FloorColor = Color.White
private val PlayerColor = Color(0xFF33CC33)
private val GemColor = Color(0xFF44BBDD)
private val StarColor = Color(0xFFFFCC00)
private val PushBlockYellow = Color(0xFFDDCC22)
private val PushBlockDefault = Color(0xFFCC8844)
private val RoundBlockColor = Color(0xFFCC6633)
private val BlackHoleColor = Color(0xFF111111)
private val MonsterColor = Color(0xFFDD2222)
private val HazardColor = Color(0xFFDD2222)
private val DoorColor = Color(0xFFDD8800)
private val KeyColor = Color(0xFFDDAA00)
private val SoftBlockColor = Color(0xFFEEDDAA)
private val TeleporterColor = Color(0xFF4488CC)
private val WallOutline = Color(0xFF8A7A5A)

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

        // Build a set of wall positions for neighbor checks
        val wallPositions = mutableSetOf<Position>()
        for (entity in state.entities.values) {
            if (entity.kind == EntityKind.Wall) {
                wallPositions.add(entity.pos)
            }
        }

        // Draw floor (white background for the whole board)
        drawRect(
            color = FloorColor,
            topLeft = Offset(offsetX, offsetY),
            size = Size(cellSize * state.board.width, cellSize * state.board.height),
        )

        // Draw entities sorted: walls first, then ground items, then movables, then player on top
        val sorted = state.entities.values.sortedBy { drawOrder(it.kind) }
        for (entity in sorted) {
            val x = offsetX + entity.pos.col * cellSize
            val y = offsetY + entity.pos.row * cellSize
            drawEntity(entity, x, y, cellSize, wallPositions, state.board.width, state.board.height)
        }
    }
}

private fun drawOrder(kind: EntityKind): Int = when (kind) {
    EntityKind.Wall -> 0
    EntityKind.SoftBlock -> 1
    EntityKind.Gem, EntityKind.StarGem -> 2
    EntityKind.PushBlock, EntityKind.RoundBlock -> 3
    EntityKind.Monster, EntityKind.Hazard -> 4
    EntityKind.Player -> 5
    else -> 3
}

private fun DrawScope.drawEntity(
    entity: Entity,
    x: Float,
    y: Float,
    cellSize: Float,
    wallPositions: Set<Position>,
    boardWidth: Int,
    boardHeight: Int,
) {
    when (entity.kind) {
        EntityKind.Wall -> drawWall(x, y, cellSize, entity.pos, wallPositions, boardWidth, boardHeight)
        EntityKind.Player -> drawPlayer(x, y, cellSize)
        EntityKind.Gem -> drawGem(x, y, cellSize)
        EntityKind.StarGem -> drawStar(x, y, cellSize)
        EntityKind.PushBlock -> drawPushBlock(x, y, cellSize)
        EntityKind.RoundBlock -> drawRoundBlock(x, y, cellSize)
        EntityKind.BlackHole -> drawBlackHole(x, y, cellSize)
        EntityKind.Monster -> drawMonster(x, y, cellSize)
        EntityKind.Hazard -> drawHazard(x, y, cellSize)
        EntityKind.Door -> drawDoor(x, y, cellSize)
        EntityKind.Key -> drawKey(x, y, cellSize)
        EntityKind.SoftBlock -> drawSoftBlock(x, y, cellSize)
        EntityKind.Teleporter -> drawTeleporter(x, y, cellSize)
        else -> drawGeneric(x, y, cellSize)
    }
}

private fun DrawScope.drawWall(
    x: Float, y: Float, cellSize: Float,
    pos: Position, wallPositions: Set<Position>,
    boardWidth: Int, boardHeight: Int,
) {
    // Fill with base wall color
    drawRect(color = WallBase, topLeft = Offset(x, y), size = Size(cellSize, cellSize))

    // Greek key / meander pattern approximation
    val s = cellSize
    val inset = s * 0.15f
    val lineW = s * 0.08f

    // Draw embossed pattern lines
    // Outer rectangle
    drawRect(
        color = WallLight,
        topLeft = Offset(x + inset, y + inset),
        size = Size(s - inset * 2, s - inset * 2),
        style = Stroke(width = lineW),
    )
    // Inner rectangle
    val inset2 = s * 0.3f
    drawRect(
        color = WallDark,
        topLeft = Offset(x + inset2, y + inset2),
        size = Size(s - inset2 * 2, s - inset2 * 2),
        style = Stroke(width = lineW),
    )
    // Connecting lines (meander-like)
    drawLine(WallLight, Offset(x + inset, y + s * 0.5f), Offset(x + inset2, y + s * 0.5f), lineW)
    drawLine(WallDark, Offset(x + s - inset, y + s * 0.5f), Offset(x + s - inset2, y + s * 0.5f), lineW)
    drawLine(WallLight, Offset(x + s * 0.5f, y + inset), Offset(x + s * 0.5f, y + inset2), lineW)
    drawLine(WallDark, Offset(x + s * 0.5f, y + s - inset), Offset(x + s * 0.5f, y + s - inset2), lineW)

    // Draw edge outlines only where wall borders empty space
    val strokeW = s * 0.04f
    fun isWall(c: Int, r: Int): Boolean {
        val wc = (c + boardWidth) % boardWidth
        val wr = (r + boardHeight) % boardHeight
        return Position(wc, wr) in wallPositions
    }

    if (!isWall(pos.col, pos.row - 1)) { // top edge exposed
        drawLine(WallOutline, Offset(x, y), Offset(x + s, y), strokeW)
    }
    if (!isWall(pos.col, pos.row + 1)) { // bottom edge exposed
        drawLine(WallOutline, Offset(x, y + s), Offset(x + s, y + s), strokeW)
    }
    if (!isWall(pos.col - 1, pos.row)) { // left edge exposed
        drawLine(WallOutline, Offset(x, y), Offset(x, y + s), strokeW)
    }
    if (!isWall(pos.col + 1, pos.row)) { // right edge exposed
        drawLine(WallOutline, Offset(x + s, y), Offset(x + s, y + s), strokeW)
    }
}

private fun DrawScope.drawPlayer(x: Float, y: Float, cellSize: Float) {
    val cx = x + cellSize / 2
    val cy = y + cellSize / 2
    val r = cellSize * 0.4f
    // Shadow
    drawCircle(color = Color(0xFF228822), radius = r, center = Offset(cx + r * 0.08f, cy + r * 0.08f))
    // Main
    drawCircle(color = PlayerColor, radius = r, center = Offset(cx, cy))
    // Highlight
    drawCircle(color = Color(0xFF66EE66), radius = r * 0.35f, center = Offset(cx - r * 0.2f, cy - r * 0.25f))
}

private fun DrawScope.drawGem(x: Float, y: Float, cellSize: Float) {
    val cx = x + cellSize / 2
    val cy = y + cellSize / 2
    val r = cellSize * 0.35f
    val path = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r, cy)
        lineTo(cx, cy + r)
        lineTo(cx - r, cy)
        close()
    }
    drawPath(path, color = GemColor)
    // Sparkle highlight
    val sr = r * 0.2f
    drawCircle(color = Color.White, radius = sr, center = Offset(cx - r * 0.2f, cy - r * 0.3f))
}

private fun DrawScope.drawStar(x: Float, y: Float, cellSize: Float) {
    val cx = x + cellSize / 2
    val cy = y + cellSize / 2
    val outer = cellSize * 0.38f
    val inner = cellSize * 0.15f
    val path = Path()
    for (i in 0 until 5) {
        val outerAngle = Math.toRadians(-90.0 + i * 72.0)
        val innerAngle = Math.toRadians(-90.0 + i * 72.0 + 36.0)
        val ox = cx + (outer * Math.cos(outerAngle)).toFloat()
        val oy = cy + (outer * Math.sin(outerAngle)).toFloat()
        val ix = cx + (inner * Math.cos(innerAngle)).toFloat()
        val iy = cy + (inner * Math.sin(innerAngle)).toFloat()
        if (i == 0) path.moveTo(ox, oy) else path.lineTo(ox, oy)
        path.lineTo(ix, iy)
    }
    path.close()
    drawPath(path, color = StarColor)
    // Sparkle
    drawCircle(color = Color.White, radius = inner * 0.5f, center = Offset(cx, cy))
}

private fun DrawScope.drawPushBlock(x: Float, y: Float, cellSize: Float) {
    val pad = cellSize * 0.04f
    val s = cellSize - pad * 2
    // Main block
    drawRect(color = PushBlockYellow, topLeft = Offset(x + pad, y + pad), size = Size(s, s))
    // Raised edge highlight
    drawLine(Color(0xFFEEDD66), Offset(x + pad, y + pad), Offset(x + pad + s, y + pad), s * 0.06f)
    drawLine(Color(0xFFEEDD66), Offset(x + pad, y + pad), Offset(x + pad, y + pad + s), s * 0.06f)
    // Shadow edge
    drawLine(Color(0xFFAA9922), Offset(x + pad + s, y + pad), Offset(x + pad + s, y + pad + s), s * 0.06f)
    drawLine(Color(0xFFAA9922), Offset(x + pad, y + pad + s), Offset(x + pad + s, y + pad + s), s * 0.06f)
}

private fun DrawScope.drawRoundBlock(x: Float, y: Float, cellSize: Float) {
    val cx = x + cellSize / 2
    val cy = y + cellSize / 2
    val r = cellSize * 0.42f
    drawCircle(color = RoundBlockColor, radius = r, center = Offset(cx, cy))
    drawCircle(color = Color(0xFFDD8844), radius = r * 0.6f, center = Offset(cx - r * 0.15f, cy - r * 0.15f))
}

private fun DrawScope.drawBlackHole(x: Float, y: Float, cellSize: Float) {
    val cx = x + cellSize / 2
    val cy = y + cellSize / 2
    val r = cellSize * 0.4f
    drawCircle(color = BlackHoleColor, radius = r, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF333333), radius = r * 0.6f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF555555), radius = r * 0.3f, center = Offset(cx, cy))
}

private fun DrawScope.drawMonster(x: Float, y: Float, cellSize: Float) {
    val cx = x + cellSize / 2
    val cy = y + cellSize / 2
    val r = cellSize * 0.38f
    // Body
    drawCircle(color = MonsterColor, radius = r, center = Offset(cx, cy))
    // Eyes
    val eyeR = r * 0.2f
    drawCircle(color = Color.White, radius = eyeR, center = Offset(cx - r * 0.3f, cy - r * 0.15f))
    drawCircle(color = Color.White, radius = eyeR, center = Offset(cx + r * 0.3f, cy - r * 0.15f))
    drawCircle(color = Color.Black, radius = eyeR * 0.5f, center = Offset(cx - r * 0.3f, cy - r * 0.15f))
    drawCircle(color = Color.Black, radius = eyeR * 0.5f, center = Offset(cx + r * 0.3f, cy - r * 0.15f))
}

private fun DrawScope.drawHazard(x: Float, y: Float, cellSize: Float) {
    val cx = x + cellSize / 2
    val cy = y + cellSize / 2
    val r = cellSize * 0.35f
    val w = cellSize * 0.08f
    drawLine(HazardColor, Offset(cx - r, cy - r), Offset(cx + r, cy + r), w)
    drawLine(HazardColor, Offset(cx + r, cy - r), Offset(cx - r, cy + r), w)
}

private fun DrawScope.drawDoor(x: Float, y: Float, cellSize: Float) {
    val pad = cellSize * 0.06f
    val s = cellSize - pad * 2
    drawRect(color = DoorColor, topLeft = Offset(x + pad, y + pad), size = Size(s, s))
    // Cross-hatch
    val lineW = cellSize * 0.04f
    drawLine(Color(0xFFBB7700), Offset(x + pad, y + pad), Offset(x + pad + s, y + pad + s), lineW)
    drawLine(Color(0xFFBB7700), Offset(x + pad + s, y + pad), Offset(x + pad, y + pad + s), lineW)
}

private fun DrawScope.drawKey(x: Float, y: Float, cellSize: Float) {
    val cx = x + cellSize / 2
    val cy = y + cellSize / 2
    // Key head (circle)
    drawCircle(color = KeyColor, radius = cellSize * 0.2f, center = Offset(cx, cy - cellSize * 0.1f))
    // Key shaft
    drawRect(color = KeyColor, topLeft = Offset(cx - cellSize * 0.04f, cy), size = Size(cellSize * 0.08f, cellSize * 0.25f))
    // Key teeth
    drawRect(color = KeyColor, topLeft = Offset(cx, cy + cellSize * 0.12f), size = Size(cellSize * 0.1f, cellSize * 0.04f))
}

private fun DrawScope.drawSoftBlock(x: Float, y: Float, cellSize: Float) {
    val pad = cellSize * 0.02f
    drawRect(color = SoftBlockColor, topLeft = Offset(x + pad, y + pad), size = Size(cellSize - pad * 2, cellSize - pad * 2))
}

private fun DrawScope.drawTeleporter(x: Float, y: Float, cellSize: Float) {
    val cx = x + cellSize / 2
    val cy = y + cellSize / 2
    // Concentric circles (bullseye)
    drawCircle(color = TeleporterColor, radius = cellSize * 0.4f, center = Offset(cx, cy))
    drawCircle(color = Color.White, radius = cellSize * 0.28f, center = Offset(cx, cy))
    drawCircle(color = TeleporterColor, radius = cellSize * 0.16f, center = Offset(cx, cy))
    drawCircle(color = Color.White, radius = cellSize * 0.06f, center = Offset(cx, cy))
}

private fun DrawScope.drawGeneric(x: Float, y: Float, cellSize: Float) {
    val pad = cellSize * 0.1f
    drawRect(
        color = Color(0xFF607D8B),
        topLeft = Offset(x + pad, y + pad),
        size = Size(cellSize - pad * 2, cellSize - pad * 2),
    )
}

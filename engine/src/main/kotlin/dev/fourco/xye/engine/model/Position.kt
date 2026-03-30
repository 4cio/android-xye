package dev.fourco.xye.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class Position(val col: Int, val row: Int) {
    fun move(dir: Direction): Position = when (dir) {
        Direction.UP    -> copy(row = row - 1)
        Direction.DOWN  -> copy(row = row + 1)
        Direction.LEFT  -> copy(col = col - 1)
        Direction.RIGHT -> copy(col = col + 1)
    }
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT;
    val opposite: Direction get() = when (this) {
        UP -> DOWN; DOWN -> UP; LEFT -> RIGHT; RIGHT -> LEFT
    }
}

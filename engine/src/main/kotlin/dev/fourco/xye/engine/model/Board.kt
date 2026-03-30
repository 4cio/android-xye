package dev.fourco.xye.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class Board(
    val width: Int,
    val height: Int,
    private val cells: Map<Position, List<EntityId>> = emptyMap(),
) {
    fun entitiesAt(pos: Position): List<EntityId> = cells[pos] ?: emptyList()
    fun isInBounds(pos: Position): Boolean = pos.col in 0 until width && pos.row in 0 until height
    fun place(id: EntityId, pos: Position): Board {
        val current = entitiesAt(pos)
        return copy(cells = cells + (pos to current + id))
    }
    fun remove(id: EntityId, pos: Position): Board {
        val current = entitiesAt(pos)
        val updated = current - id
        return if (updated.isEmpty()) copy(cells = cells - pos)
        else copy(cells = cells + (pos to updated))
    }
}

package dev.fourco.xye.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val levelId: String,
    val tick: Long,
    val board: Board,
    val entities: Map<EntityId, Entity>,
    val inventory: Inventory,
    val goals: Goals,
    val status: GameStatus,
    val nextEntityId: Int,
    val pendingInput: InputIntent? = null,
) {
    fun entity(id: EntityId): Entity? = entities[id]
    fun player(): Entity? = entities.values.find { it.kind == EntityKind.Player }
    fun updateEntity(entity: Entity): GameState = copy(entities = entities + (entity.id to entity))
    fun removeEntity(id: EntityId): GameState {
        val entity = entities[id] ?: return this
        return copy(entities = entities - id, board = board.remove(id, entity.pos))
    }
    fun moveEntity(id: EntityId, to: Position): GameState {
        val entity = entities[id] ?: return this
        val newBoard = board.remove(id, entity.pos).place(id, to)
        val newEntity = entity.copy(pos = to)
        return copy(entities = entities + (id to newEntity), board = newBoard)
    }
}

@Serializable
enum class GameStatus { Playing, Won, Lost, Paused }

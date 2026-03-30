package dev.fourco.xye.content

import dev.fourco.xye.engine.model.*

data class RuntimeLevel(
    val meta: LevelMeta,
    val width: Int,
    val height: Int,
    val entities: List<Entity>,
    val playerStart: Position,
    val totalGems: Int,
    val totalStars: Int,
) {
    fun toInitialState(): GameState {
        val entityMap = entities.associateBy { it.id }
        var board = Board(width, height)
        for (entity in entities) {
            board = board.place(entity.id, entity.pos)
        }
        return GameState(
            levelId = meta.id,
            tick = 0,
            board = board,
            entities = entityMap,
            inventory = Inventory(),
            goals = Goals(totalGems = totalGems, totalStars = totalStars),
            status = GameStatus.Playing,
            nextEntityId = entities.size,
        )
    }
}

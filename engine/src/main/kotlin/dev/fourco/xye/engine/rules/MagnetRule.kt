package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*

/**
 * Magnets attract adjacent entities along their axis each tick.
 *
 * MagnetH affects entities to the left and right (scans along columns).
 * MagnetV affects entities above and below (scans along rows).
 *
 * For each direction, the magnet scans outward up to range 2.
 * If an entity is found, it is pulled one cell closer to the magnet,
 * provided the destination cell is empty/passable and not the magnet itself.
 *
 * Objects NOT affected: Player, Wall, BlackHole, Gem, StarGem, SoftBlock.
 */
class MagnetRule : GameRule {

    companion object {
        private val IMMUNE_KINDS = setOf(
            EntityKind.Player,
            EntityKind.Wall,
            EntityKind.BlackHole,
            EntityKind.Gem,
            EntityKind.StarGem,
            EntityKind.SoftBlock,
        )

        private val BLOCKING_KINDS = setOf(
            EntityKind.Wall,
            EntityKind.PushBlock,
            EntityKind.RoundBlock,
            EntityKind.Door,
            EntityKind.BlackHole,
            EntityKind.MagnetH,
            EntityKind.MagnetV,
        )

        private const val MAGNET_RANGE = 2
    }

    override fun apply(state: GameState): GameState {
        val magnets = state.entities.values.filter {
            it.kind == EntityKind.MagnetH || it.kind == EntityKind.MagnetV
        }
        if (magnets.isEmpty()) return state

        // Collect all intended moves to avoid order-dependent conflicts.
        val moves = mutableMapOf<EntityId, Position>()

        for (magnet in magnets) {
            val directions = when (magnet.kind) {
                EntityKind.MagnetH -> listOf(Direction.LEFT, Direction.RIGHT)
                EntityKind.MagnetV -> listOf(Direction.UP, Direction.DOWN)
                else -> continue
            }

            for (dir in directions) {
                // Scan outward from magnet along the axis, up to MAGNET_RANGE cells.
                for (dist in 1..MAGNET_RANGE) {
                    var scanPos = magnet.pos
                    repeat(dist) { scanPos = scanPos.move(dir) }
                    if (!state.board.isInBounds(scanPos)) break

                    val entitiesAtScan = state.board.entitiesAt(scanPos)
                        .mapNotNull { state.entity(it) }

                    // If there's a wall or other blocking entity between the magnet and
                    // the scanned position, stop scanning further in this direction.
                    val hasBlocker = entitiesAtScan.any { it.kind in BLOCKING_KINDS }

                    for (entity in entitiesAtScan) {
                        if (entity.kind in IMMUNE_KINDS) continue
                        if (entity.id in moves) continue

                        // Pull one cell closer to the magnet.
                        val destination = entity.pos.move(dir.opposite)
                        if (!state.board.isInBounds(destination)) continue
                        if (destination == magnet.pos) continue

                        if (isPassable(state, destination)) {
                            moves[entity.id] = destination
                        }
                    }

                    // Stop scanning if a blocking entity was found at this distance.
                    if (hasBlocker) break
                }
            }
        }

        // Apply all collected moves.
        var next = state
        for ((entityId, target) in moves) {
            next = next.moveEntity(entityId, target)
        }
        return next
    }

    private fun isPassable(state: GameState, pos: Position): Boolean {
        if (!state.board.isInBounds(pos)) return false
        val entities = state.board.entitiesAt(pos).mapNotNull { state.entity(it) }
        return entities.none { it.kind in BLOCKING_KINDS }
    }
}

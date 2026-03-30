package dev.fourco.xye.content

import dev.fourco.xye.engine.model.*
import java.io.InputStream

class KyeParser : LevelParser {
    override val format = LevelFormat.KYE

    companion object {
        const val KYE_WIDTH = 30
        const val KYE_HEIGHT = 20
    }

    override fun parsePack(name: String, input: InputStream): LevelPack {
        val lines = input.bufferedReader().readLines().map { it.trimEnd('\r') }
        val levelCount = lines.first().trim().toInt()
        val metas = mutableListOf<LevelMeta>()

        var lineIdx = 1
        for (i in 0 until levelCount) {
            if (lineIdx >= lines.size) break
            val levelName = lines[lineIdx++]
            val hint = if (lineIdx < lines.size) lines[lineIdx++] else ""
            val bye = if (lineIdx < lines.size) lines[lineIdx++] else ""
            lineIdx += KYE_HEIGHT // skip grid rows

            metas.add(LevelMeta(
                index = i,
                id = "${name}_$i",
                name = levelName.trim(),
                hint = hint.trim(),
                width = KYE_WIDTH,
                height = KYE_HEIGHT,
            ))
        }

        return LevelPack(
            id = name,
            name = name,
            format = LevelFormat.KYE,
            levels = metas,
        )
    }

    override fun parseLevel(input: InputStream, index: Int): RuntimeLevel {
        val lines = input.bufferedReader().readLines().map { it.trimEnd('\r') }
        val levelCount = lines.first().trim().toInt()
        require(index in 0 until levelCount) { "Level index $index out of range ($levelCount levels)" }

        var lineIdx = 1
        for (i in 0 until index) {
            lineIdx += 3 + KYE_HEIGHT
        }

        val levelName = lines[lineIdx++].trim()
        val hint = lines[lineIdx++].trim()
        val bye = lines[lineIdx++].trim()

        val gridRows = (0 until KYE_HEIGHT).map { r ->
            val line = if (lineIdx + r < lines.size) lines[lineIdx + r] else ""
            line.padEnd(KYE_WIDTH)
        }

        return buildLevel(gridRows, index, levelName, hint)
    }

    private fun buildLevel(
        rows: List<String>,
        index: Int,
        name: String,
        hint: String,
    ): RuntimeLevel {
        val entities = mutableListOf<Entity>()
        var nextId = 0
        var playerStart: Position? = null
        var totalGems = 0
        var totalStars = 0

        fun add(kind: EntityKind, col: Int, row: Int, props: EntityProps = EntityProps()) {
            val id = EntityId(nextId++)
            entities.add(Entity(id, kind, Position(col, row), props))
        }

        for (row in rows.indices) {
            val line = rows[row]
            for (col in 0 until minOf(line.length, KYE_WIDTH)) {
                // Real Kye character map (from original Kye game by Colin Garbutt)
                when (line[col]) {
                    'K' -> { add(EntityKind.Player, col, row); playerStart = Position(col, row) }

                    // Walls
                    '5' -> add(EntityKind.Wall, col, row)
                    '#' -> add(EntityKind.Wall, col, row)

                    // Collectibles: * = diamond (the main gem to collect)
                    '*' -> { add(EntityKind.Gem, col, row); totalGems++ }

                    // Blocks
                    'B' -> add(EntityKind.PushBlock, col, row)
                    'b' -> add(EntityKind.RoundBlock, col, row)

                    // Earth (consumable on walk-through)
                    'e' -> add(EntityKind.SoftBlock, col, row)

                    // Sliders (move in one direction, stop when blocked)
                    '1' -> add(EntityKind.SliderRight, col, row)
                    '2' -> add(EntityKind.SliderDown, col, row)
                    '3' -> add(EntityKind.SliderLeft, col, row)
                    '4' -> add(EntityKind.SliderUp, col, row)

                    // Rockies (move in one direction, reverse when blocked)
                    'u' -> add(EntityKind.RockyUp, col, row)
                    'd' -> add(EntityKind.RockyDown, col, row)
                    'l' -> add(EntityKind.RockyLeft, col, row)
                    'r' -> add(EntityKind.RockyRight, col, row)
                    'R' -> add(EntityKind.RockyRight, col, row)

                    // Hazards
                    'H' -> add(EntityKind.BlackHole, col, row)
                    'T' -> add(EntityKind.TimerBlock, col, row, EntityProps(timerTicks = 9))

                    // Monsters
                    'S' -> add(EntityKind.Monster, col, row) // Sentry
                    'F' -> add(EntityKind.Monster, col, row) // Gnasher
                    'M' -> add(EntityKind.Monster, col, row) // Monster
                    '~' -> add(EntityKind.Hazard, col, row)

                    // Magnets
                    'h' -> add(EntityKind.MagnetH, col, row)
                    'v' -> add(EntityKind.MagnetV, col, row)

                    // Turners (map to generic for now)
                    'a' -> {} // anti-clockwise turner - skip for now
                    'c' -> {} // clockwise turner - skip for now

                    // One-way doors (ground objects - skip for now)
                    '^', 'V', '<', '>' -> {}

                    '.', ' ' -> { /* empty */ }
                    else -> { /* unknown char, treat as empty */ }
                }
            }
        }

        requireNotNull(playerStart) { "No player (K) found in level $index" }

        val meta = LevelMeta(
            index = index,
            id = "kye_$index",
            name = name,
            hint = hint,
            width = KYE_WIDTH,
            height = KYE_HEIGHT,
        )

        return RuntimeLevel(
            meta = meta,
            width = KYE_WIDTH,
            height = KYE_HEIGHT,
            entities = entities,
            playerStart = playerStart,
            totalGems = totalGems,
            totalStars = totalStars,
        )
    }
}

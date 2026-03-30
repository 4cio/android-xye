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
        val lines = input.bufferedReader().readLines()
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
        val lines = input.bufferedReader().readLines()
        val levelCount = lines.first().trim().toInt()
        require(index in 0 until levelCount) { "Level index $index out of range ($levelCount levels)" }

        // Skip to the right level
        var lineIdx = 1
        for (i in 0 until index) {
            lineIdx += 3 + KYE_HEIGHT // name + hint + bye + 20 rows
        }

        val levelName = lines[lineIdx++].trim()
        val hint = lines[lineIdx++].trim()
        val bye = lines[lineIdx++].trim()

        val gridRows = (0 until KYE_HEIGHT).map { r ->
            val line = if (lineIdx + r < lines.size) lines[lineIdx + r] else ""
            line.padEnd(KYE_WIDTH) // pad short lines with spaces
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
                when (line[col]) {
                    'K' -> { add(EntityKind.Player, col, row); playerStart = Position(col, row) }
                    '#' -> add(EntityKind.Wall, col, row)
                    'd' -> { add(EntityKind.Gem, col, row); totalGems++ }
                    '*' -> { add(EntityKind.StarGem, col, row); totalStars++ }
                    'B' -> add(EntityKind.PushBlock, col, row)
                    'R' -> add(EntityKind.RoundBlock, col, row)
                    'b' -> add(EntityKind.SoftBlock, col, row)
                    '1' -> add(EntityKind.SliderRight, col, row)
                    '2' -> add(EntityKind.SliderDown, col, row)
                    '3' -> add(EntityKind.SliderLeft, col, row)
                    '4' -> add(EntityKind.SliderUp, col, row)
                    '5' -> add(EntityKind.RockyRight, col, row)
                    '6' -> add(EntityKind.RockyDown, col, row)
                    '7' -> add(EntityKind.RockyLeft, col, row)
                    '8' -> add(EntityKind.RockyUp, col, row)
                    'H' -> add(EntityKind.BlackHole, col, row)
                    'T' -> add(EntityKind.TimerBlock, col, row, EntityProps(timerTicks = 9))
                    'S' -> add(EntityKind.Shooter, col, row, EntityProps(
                        shootInterval = 10,
                        shootKind = EntityKind.SliderRight,
                        direction = Direction.RIGHT,
                    ))
                    'M' -> add(EntityKind.Monster, col, row)
                    '~' -> add(EntityKind.Hazard, col, row)
                    'h' -> add(EntityKind.MagnetH, col, row)
                    'v' -> add(EntityKind.MagnetV, col, row)
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

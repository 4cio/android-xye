package dev.fourco.xye.content

import dev.fourco.xye.engine.model.*
import java.io.InputStream

class XsbParser : LevelParser {
    override val format = LevelFormat.XSB

    override fun parsePack(name: String, input: InputStream): LevelPack {
        val text = input.bufferedReader().readText()
        val levels = splitLevels(text)
        val metas = levels.mapIndexed { i, raw ->
            LevelMeta(
                index = i,
                id = "${name}_$i",
                name = raw.title ?: "Level ${i + 1}",
                width = raw.width,
                height = raw.height,
            )
        }
        return LevelPack(
            id = name,
            name = name,
            format = LevelFormat.XSB,
            levels = metas,
        )
    }

    override fun parseLevel(input: InputStream, index: Int): RuntimeLevel {
        val text = input.bufferedReader().readText()
        val levels = splitLevels(text)
        require(index in levels.indices) { "Level index $index out of range (${levels.size} levels)" }
        return buildRuntimeLevel(levels[index], index)
    }

    private data class RawLevel(
        val rows: List<String>,
        val title: String?,
        val width: Int,
        val height: Int,
    )

    private fun splitLevels(text: String): List<RawLevel> {
        val levels = mutableListOf<RawLevel>()
        val currentRows = mutableListOf<String>()
        var title: String? = null

        for (rawLine in text.lines()) {
            val line = rawLine.trimEnd()

            // Comment or metadata line
            if (line.startsWith(";")) {
                continue
            }

            // Title line (starts with letter, no level chars)
            if (line.isNotEmpty() && !isLevelLine(line)) {
                if (currentRows.isNotEmpty()) {
                    // End current level
                    levels.add(finalizeRaw(currentRows, title))
                    currentRows.clear()
                }
                title = line.trim()
                continue
            }

            // Level line
            if (line.isNotEmpty() && isLevelLine(line)) {
                // Expand RLE: e.g., "3#" -> "###"
                val expanded = expandRle(line)
                // Handle pipe as row separator
                if (expanded.contains('|')) {
                    expanded.split('|').forEach { part ->
                        if (part.isNotEmpty()) currentRows.add(part)
                    }
                } else {
                    currentRows.add(expanded)
                }
            } else if (line.isEmpty() && currentRows.isNotEmpty()) {
                // Blank line ends a level
                levels.add(finalizeRaw(currentRows, title))
                currentRows.clear()
                title = null
            }
        }

        if (currentRows.isNotEmpty()) {
            levels.add(finalizeRaw(currentRows, title))
        }

        return levels
    }

    private fun finalizeRaw(rows: List<String>, title: String?): RawLevel {
        val width = rows.maxOf { it.length }
        return RawLevel(rows = rows.toList(), title = title, width = width, height = rows.size)
    }

    private fun isLevelLine(line: String): Boolean {
        // A level line contains only valid XSB characters (possibly with RLE digits)
        return line.all { it in "#@+\$.*- _" || it.isDigit() || it == '|' }
    }

    private fun expandRle(line: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < line.length) {
            if (line[i].isDigit()) {
                val start = i
                while (i < line.length && line[i].isDigit()) i++
                val count = line.substring(start, i).toInt()
                if (i < line.length) {
                    repeat(count) { sb.append(line[i]) }
                    i++
                }
            } else {
                sb.append(line[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun buildRuntimeLevel(raw: RawLevel, index: Int): RuntimeLevel {
        val entities = mutableListOf<Entity>()
        var nextId = 0
        var playerStart: Position? = null
        var totalGems = 0

        fun addEntity(kind: EntityKind, col: Int, row: Int, props: EntityProps = EntityProps()): Entity {
            val id = EntityId(nextId++)
            val entity = Entity(id, kind, Position(col, row), props)
            entities.add(entity)
            return entity
        }

        for (row in 0 until raw.height) {
            val line = raw.rows[row]
            for (col in line.indices) {
                when (line[col]) {
                    '#' -> addEntity(EntityKind.Wall, col, row)
                    '@' -> {
                        addEntity(EntityKind.Player, col, row)
                        playerStart = Position(col, row)
                    }
                    '+' -> {
                        // Player on goal
                        addEntity(EntityKind.Player, col, row)
                        addEntity(EntityKind.Gem, col, row)
                        playerStart = Position(col, row)
                        totalGems++
                    }
                    '$' -> addEntity(EntityKind.PushBlock, col, row)
                    '.' -> {
                        addEntity(EntityKind.Gem, col, row)
                        totalGems++
                    }
                    '*' -> {
                        // Box on goal
                        addEntity(EntityKind.PushBlock, col, row)
                        addEntity(EntityKind.Gem, col, row)
                        totalGems++
                    }
                    ' ', '-', '_' -> { /* empty floor */ }
                }
            }
        }

        requireNotNull(playerStart) { "No player (@) found in level $index" }

        val meta = LevelMeta(
            index = index,
            id = "xsb_$index",
            name = raw.title ?: "Level ${index + 1}",
            width = raw.width,
            height = raw.height,
        )

        return RuntimeLevel(
            meta = meta,
            width = raw.width,
            height = raw.height,
            entities = entities,
            playerStart = playerStart,
            totalGems = totalGems,
            totalStars = 0,
        )
    }
}

package dev.fourco.xye.content

import dev.fourco.xye.engine.model.*
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class XyeParser : LevelParser {
    override val format = LevelFormat.XYE

    override fun parsePack(name: String, input: InputStream): LevelPack {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
        val root = doc.documentElement // <pack>

        val packName = root.getAttribute("name").ifEmpty { name }
        val author = root.getAttribute("author").orEmpty()

        val levelElements = root.getElementsByTagName("level")
        val metas = (0 until levelElements.length).map { i ->
            val el = levelElements.item(i) as Element
            LevelMeta(
                index = i,
                id = "${name}_$i",
                name = el.getAttribute("name").ifEmpty { "Level ${i + 1}" },
                hint = "",
                width = el.getAttribute("width").toIntOrNull() ?: 30,
                height = el.getAttribute("height").toIntOrNull() ?: 20,
            )
        }

        return LevelPack(
            id = name,
            name = packName,
            author = author,
            format = LevelFormat.XYE,
            levels = metas,
        )
    }

    override fun parseLevel(input: InputStream, index: Int): RuntimeLevel {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
        val root = doc.documentElement

        val levelElements = root.getElementsByTagName("level")
        require(index in 0 until levelElements.length) {
            "Level index $index out of range (${levelElements.length} levels)"
        }

        val levelEl = levelElements.item(index) as Element
        val width = levelEl.getAttribute("width").toIntOrNull() ?: 30
        val height = levelEl.getAttribute("height").toIntOrNull() ?: 20
        val levelName = levelEl.getAttribute("name").ifEmpty { "Level ${index + 1}" }

        // Find the objects container - could be <objects>, <normal>, or direct children
        val objectsEl = getObjectsContainer(levelEl)

        val entities = mutableListOf<Entity>()
        var nextId = 0
        var playerStart: Position? = null
        var totalGems = 0
        var totalStars = 0

        fun add(kind: EntityKind, x: Int, y: Int, props: EntityProps = EntityProps()) {
            val id = EntityId(nextId++)
            entities.add(Entity(id, kind, Position(x, y), props))
        }

        // Parse each child element
        val children = objectsEl.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node !is Element) continue

            val x = node.getAttribute("x").toIntOrNull() ?: continue
            val y = node.getAttribute("y").toIntOrNull() ?: continue
            val bc = parseColor(node.getAttribute("bc"))
            val dir = parseDirection(node.getAttribute("dir"))
            val round = node.getAttribute("round") == "1"

            when (node.tagName.lowercase()) {
                "xye" -> {
                    add(EntityKind.Player, x, y)
                    playerStart = Position(x, y)
                }
                "wall" -> add(EntityKind.Wall, x, y)
                "block" -> {
                    val kind = if (round) EntityKind.RoundBlock else EntityKind.PushBlock
                    add(kind, x, y, EntityProps(color = bc, isRound = round))
                }
                "gem" -> {
                    add(EntityKind.Gem, x, y, EntityProps(color = bc))
                    totalGems++
                }
                "star" -> {
                    add(EntityKind.StarGem, x, y)
                    totalStars++
                }
                "earth" -> add(EntityKind.SoftBlock, x, y)
                "key" -> add(EntityKind.Key, x, y, EntityProps(color = bc))
                "lock" -> add(EntityKind.Door, x, y, EntityProps(color = bc))
                "teleport" -> {
                    val pair = node.getAttribute("pair").toIntOrNull() ?: 0
                    add(EntityKind.Teleporter, x, y, EntityProps(pairId = pair, direction = dir))
                }
                "dangerous" -> add(EntityKind.BlackHole, x, y)
                "mine" -> add(EntityKind.Hazard, x, y)
                "beast" -> add(EntityKind.Monster, x, y)
                "timer" -> {
                    val value = node.getAttribute("value").toIntOrNull() ?: 9
                    add(EntityKind.TimerBlock, x, y, EntityProps(timerTicks = value, color = bc))
                }
                "impacter" -> add(EntityKind.Shooter, x, y, EntityProps(
                    direction = dir ?: Direction.RIGHT,
                    shootInterval = 10,
                    shootKind = EntityKind.SliderRight,
                    color = bc,
                ))
                "arrow" -> {
                    val kind = when (dir) {
                        Direction.UP -> EntityKind.SliderUp
                        Direction.DOWN -> EntityKind.SliderDown
                        Direction.LEFT -> EntityKind.SliderLeft
                        else -> EntityKind.SliderRight
                    }
                    add(kind, x, y)
                }
                "autoarrow" -> {
                    val kind = when (dir) {
                        Direction.UP -> EntityKind.RockyUp
                        Direction.DOWN -> EntityKind.RockyDown
                        Direction.LEFT -> EntityKind.RockyLeft
                        else -> EntityKind.RockyRight
                    }
                    add(kind, x, y)
                }
                "magnetic" -> {
                    val kindAttr = node.getAttribute("kind").lowercase()
                    val kind = if (kindAttr.contains("v") || kindAttr.contains("vert"))
                        EntityKind.MagnetV else EntityKind.MagnetH
                    add(kind, x, y)
                }
                // Ground objects - skip for now
                "hint", "tdoor", "portal", "marked", "blockdoor", "firepad", "pit" -> { }
            }
        }

        requireNotNull(playerStart) { "No player (xye) found in level $index" }

        val meta = LevelMeta(
            index = index,
            id = "${root.getAttribute("name").ifEmpty { "xye" }}_$index",
            name = levelName,
            width = width,
            height = height,
        )

        return RuntimeLevel(
            meta = meta,
            width = width,
            height = height,
            entities = entities,
            playerStart = playerStart,
            totalGems = totalGems,
            totalStars = totalStars,
        )
    }

    private fun getObjectsContainer(levelEl: Element): Element {
        // Try <objects>, <normal>, or fall back to level element itself
        for (tag in listOf("objects", "normal", "ground")) {
            val nodes = levelEl.getElementsByTagName(tag)
            if (nodes.length > 0) return nodes.item(0) as Element
        }
        return levelEl
    }

    private fun parseColor(bc: String?): Int? = when (bc?.uppercase()) {
        "Y" -> 0
        "R" -> 1
        "B" -> 2
        "G" -> 3
        "P" -> 4
        else -> null
    }

    private fun parseDirection(dir: String?): Direction? = when (dir?.uppercase()) {
        "U" -> Direction.UP
        "D" -> Direction.DOWN
        "L" -> Direction.LEFT
        "R" -> Direction.RIGHT
        else -> null
    }
}

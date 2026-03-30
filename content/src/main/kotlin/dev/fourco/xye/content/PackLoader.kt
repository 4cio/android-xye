package dev.fourco.xye.content

import java.io.File

class PackLoader(
    private val parsers: Map<LevelFormat, LevelParser> = mapOf(
        LevelFormat.XYE to XyeParser(),
        LevelFormat.KYE to KyeParser(),
        LevelFormat.XSB to XsbParser(),
    ),
) {
    fun loadFromResource(path: String): LevelPack {
        val stream = javaClass.classLoader.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Resource not found: $path")
        val format = detectFormat(path)
        val parser = parsers[format]
            ?: throw IllegalArgumentException("No parser for format: $format")
        return parser.parsePack(extractName(path), stream)
    }

    fun loadFromFile(file: File): LevelPack {
        val format = detectFormat(file.name)
        val parser = parsers[format]
            ?: throw IllegalArgumentException("No parser for format: $format")
        return parser.parsePack(file.nameWithoutExtension, file.inputStream())
    }

    fun loadLevelFromResource(path: String, index: Int): RuntimeLevel {
        val stream = javaClass.classLoader.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Resource not found: $path")
        val format = detectFormat(path)
        val parser = parsers[format]
            ?: throw IllegalArgumentException("No parser for format: $format")
        return parser.parseLevel(stream, index)
    }

    fun loadLevelFromFile(file: File, index: Int): RuntimeLevel {
        val format = detectFormat(file.name)
        val parser = parsers[format]
            ?: throw IllegalArgumentException("No parser for format: $format")
        return parser.parseLevel(file.inputStream(), index)
    }

    fun detectFormat(filename: String): LevelFormat = when {
        filename.endsWith(".xye", ignoreCase = true) -> LevelFormat.XYE
        filename.endsWith(".kye", ignoreCase = true) -> LevelFormat.KYE
        filename.endsWith(".xsb", ignoreCase = true) -> LevelFormat.XSB
        filename.endsWith(".sok", ignoreCase = true) -> LevelFormat.XSB
        else -> throw IllegalArgumentException("Unknown level format: $filename")
    }

    private fun extractName(path: String): String {
        val filename = path.substringAfterLast('/').substringAfterLast('\\')
        return filename.substringBeforeLast('.')
    }
}

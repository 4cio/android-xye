package dev.fourco.xye.content

import java.io.InputStream

interface LevelParser {
    val format: LevelFormat
    fun parsePack(name: String, input: InputStream): LevelPack
    fun parseLevel(input: InputStream, index: Int): RuntimeLevel
}

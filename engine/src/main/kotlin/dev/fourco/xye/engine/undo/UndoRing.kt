package dev.fourco.xye.engine.undo

import dev.fourco.xye.engine.model.GameState

class UndoRing(private val capacity: Int = 512) {
    private val buffer = arrayOfNulls<GameState>(capacity)
    private var head = 0
    private var size = 0

    fun push(state: GameState) {
        buffer[head] = state
        head = (head + 1) % capacity
        if (size < capacity) size++
    }

    fun pop(): GameState? {
        if (size == 0) return null
        head = (head - 1 + capacity) % capacity
        size--
        val state = buffer[head]
        buffer[head] = null
        return state
    }

    fun clear() {
        buffer.fill(null)
        head = 0
        size = 0
    }

    fun isEmpty(): Boolean = size == 0
    fun currentSize(): Int = size

    fun toList(): List<GameState> {
        val result = mutableListOf<GameState>()
        var idx = (head - 1 + capacity) % capacity
        repeat(size) {
            buffer[idx]?.let { result.add(it) }
            idx = (idx - 1 + capacity) % capacity
        }
        return result
    }

    fun fromList(states: List<GameState>) {
        clear()
        states.asReversed().forEach { push(it) }
    }
}

package dev.fourco.xye.engine.replay

import dev.fourco.xye.engine.model.InputIntent

class ReplayRecorder {
    private val _entries = mutableListOf<Pair<Long, InputIntent>>()

    fun record(tick: Long, input: InputIntent) {
        _entries.add(tick to input)
    }

    fun entries(): List<Pair<Long, InputIntent>> = _entries.toList()

    fun clear() = _entries.clear()
}

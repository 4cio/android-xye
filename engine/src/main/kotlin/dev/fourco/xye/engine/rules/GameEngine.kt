package dev.fourco.xye.engine.rules

import dev.fourco.xye.engine.model.*
import dev.fourco.xye.engine.undo.UndoRing
import dev.fourco.xye.engine.replay.ReplayRecorder

class GameEngine(
    initialState: GameState,
    private val undoCapacity: Int = 512,
) {
    private var _state: GameState = initialState
    val state: GameState get() = _state

    private val undoRing = UndoRing(undoCapacity)
    private val recorder = ReplayRecorder()

    private val rules: List<GameRule> = listOf(
        MovementRule(),
        MagnetRule(),
        TriggerRule(),
        TeleportRule(),
        AutonomousRule(),
        ShooterRule(),
        TimerRule(),
        CollisionRule(),
        CollectionRule(),
        WinLossRule(),
    )

    fun tick(input: InputIntent): GameState {
        if (_state.status != GameStatus.Playing) return _state

        recorder.record(_state.tick, input)

        if (input is InputIntent.Undo) {
            val previous = undoRing.pop()
            if (previous != null) _state = previous
            return _state
        }

        undoRing.push(_state)

        var next = _state.copy(pendingInput = input)
        for (rule in rules) {
            next = rule.apply(next)
            if (next.status != GameStatus.Playing) break
        }

        _state = next.copy(tick = next.tick + 1, pendingInput = null)
        return _state
    }

    fun restore(state: GameState, undoHistory: List<GameState>) {
        _state = state
        undoRing.fromList(undoHistory)
    }

    fun undoHistory(): List<GameState> = undoRing.toList()
    fun replayLog(): List<Pair<Long, InputIntent>> = recorder.entries()
}

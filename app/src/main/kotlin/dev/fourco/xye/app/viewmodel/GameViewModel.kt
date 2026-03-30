package dev.fourco.xye.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fourco.xye.engine.model.*
import dev.fourco.xye.engine.rules.GameEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(private val initialState: GameState) : ViewModel() {

    private var engine = GameEngine(initialState)

    private val _state = MutableStateFlow(engine.state)
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val inputChannel = Channel<InputIntent>(Channel.CONFLATED)
    private var loopJob: Job? = null

    companion object {
        // Original Xye: 20 FPS = 50ms per frame. But player moves every 2 ticks.
        // Use 250ms for a comfortable puzzle pace.
        const val TICK_INTERVAL_MS = 250L
    }

    init {
        startLoop()
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            while (isActive) {
                val input = inputChannel.tryReceive().getOrNull() ?: InputIntent.Wait
                val newState = engine.tick(input)
                _state.value = newState
                if (newState.status != GameStatus.Playing) break
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    fun onInput(intent: InputIntent) {
        inputChannel.trySend(intent)
    }

    fun reset() {
        engine = GameEngine(initialState)
        _state.value = engine.state
        startLoop()
    }
}

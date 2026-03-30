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

class GameViewModel(initialState: GameState) : ViewModel() {

    private val engine = GameEngine(initialState)

    private val _state = MutableStateFlow(engine.state)
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val inputChannel = Channel<InputIntent>(Channel.CONFLATED)
    private var loopJob: Job? = null

    companion object {
        const val TICK_INTERVAL_MS = 150L
    }

    init {
        startLoop()
    }

    private fun startLoop() {
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
}

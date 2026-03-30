package dev.fourco.xye.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import dev.fourco.xye.app.ui.component.BoardCanvas
import dev.fourco.xye.app.ui.component.DPad
import dev.fourco.xye.app.ui.input.swipeInput
import dev.fourco.xye.app.viewmodel.GameViewModel
import dev.fourco.xye.engine.model.GameStatus
import dev.fourco.xye.engine.model.InputIntent

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F23))
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val intent = when (event.key) {
                        Key.DirectionUp -> InputIntent.MoveUp
                        Key.DirectionDown -> InputIntent.MoveDown
                        Key.DirectionLeft -> InputIntent.MoveLeft
                        Key.DirectionRight -> InputIntent.MoveRight
                        Key.Z -> InputIntent.Undo
                        else -> null
                    }
                    if (intent != null) {
                        viewModel.onInput(intent)
                        true
                    } else false
                } else false
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // HUD bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.levelId,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = "Gems: ${state.goals.collectedGems}/${state.goals.totalGems}",
                color = Color(0xFF00BCD4),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        // Status banner
        when (state.status) {
            GameStatus.Won -> {
                Text(
                    text = "Level Complete!",
                    color = Color(0xFF00E676),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(8.dp),
                )
            }
            GameStatus.Lost -> {
                Text(
                    text = "Game Over",
                    color = Color(0xFFFF1744),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(8.dp),
                )
            }
            else -> {}
        }

        // Board
        BoardCanvas(
            state = state,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .swipeInput(onInput = viewModel::onInput),
        )

        // Star count (if any)
        if (state.goals.totalStars > 0) {
            Text(
                text = "Stars: ${state.goals.collectedStars}/${state.goals.totalStars}",
                color = Color(0xFFFFD700),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(4.dp),
            )
        }

        // D-Pad + Undo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DPad(onInput = viewModel::onInput)

            FilledTonalButton(
                onClick = { viewModel.onInput(InputIntent.Undo) },
            ) {
                Text("Undo")
            }
        }
    }
}

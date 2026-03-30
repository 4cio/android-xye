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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fourco.xye.app.ui.component.BoardCanvas
import dev.fourco.xye.app.ui.component.DPad
import dev.fourco.xye.app.ui.input.swipeInput
import dev.fourco.xye.app.viewmodel.GameViewModel
import dev.fourco.xye.engine.model.GameStatus
import dev.fourco.xye.engine.model.InputIntent

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onQuit: () -> Unit = {},
    onReset: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE8E0D0))
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val intent = when (event.key) {
                        Key.DirectionUp -> InputIntent.MoveUp
                        Key.DirectionDown -> InputIntent.MoveDown
                        Key.DirectionLeft -> InputIntent.MoveLeft
                        Key.DirectionRight -> InputIntent.MoveRight
                        Key.Z -> InputIntent.Undo
                        Key.R -> { onReset(); null }
                        Key.Escape -> { onQuit(); null }
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
                .background(Color(0xFFDDD0B8))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onQuit) {
                Text("\u2190 Quit", color = Color(0xFF4A3728), fontSize = 14.sp)
            }

            Text(
                text = state.levelId,
                color = Color(0xFF4A3728),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )

            Text(
                text = "Gems: ${state.goals.collectedGems}/${state.goals.totalGems}",
                color = Color(0xFF2288AA),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        // Status banner
        when (state.status) {
            GameStatus.Won -> {
                Text(
                    text = "Level Complete!",
                    color = Color(0xFF33CC33),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp),
                )
            }
            GameStatus.Lost -> {
                Text(
                    text = "Game Over",
                    color = Color(0xFFDD2222),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
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
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .swipeInput(onInput = viewModel::onInput),
        )

        // Star count (if any)
        if (state.goals.totalStars > 0) {
            Text(
                text = "Stars: ${state.goals.collectedStars}/${state.goals.totalStars}",
                color = Color(0xFFDDAA00),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(2.dp),
            )
        }

        // Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DPad(onInput = viewModel::onInput)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = { viewModel.onInput(InputIntent.Undo) }) {
                    Text("Undo")
                }
                OutlinedButton(onClick = onReset) {
                    Text("Reset")
                }
            }
        }
    }
}

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
    ) {
        // Compact HUD bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFDDD0B8))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onQuit,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text("\u2190", color = Color(0xFF4A3728), fontSize = 16.sp)
            }

            Text(
                text = state.levelId,
                color = Color(0xFF4A3728),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            // Gem count
            Text(
                text = "\u25C6 ${state.goals.collectedGems}/${state.goals.totalGems}",
                color = Color(0xFF2288AA),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )

            // Star count inline
            if (state.goals.totalStars > 0) {
                Text(
                    text = "  \u2605 ${state.goals.collectedStars}/${state.goals.totalStars}",
                    color = Color(0xFFDDAA00),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Undo button
            TextButton(
                onClick = { viewModel.onInput(InputIntent.Undo) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text("Undo", color = Color(0xFF4A3728), fontSize = 13.sp)
            }

            // Reset button
            TextButton(
                onClick = onReset,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text("Reset", color = Color(0xFF8A7A5A), fontSize = 13.sp)
            }
        }

        // Board fills everything else
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            BoardCanvas(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .swipeInput(onInput = viewModel::onInput),
            )

            // Won/Lost overlay
            when (state.status) {
                GameStatus.Won -> StatusOverlay("Level Complete!", Color(0xFF33CC33))
                GameStatus.Lost -> StatusOverlay("Game Over", Color(0xFFDD2222))
                else -> {}
            }
        }
    }
}

@Composable
private fun StatusOverlay(text: String, color: Color) {
    Surface(
        color = Color.White.copy(alpha = 0.85f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}

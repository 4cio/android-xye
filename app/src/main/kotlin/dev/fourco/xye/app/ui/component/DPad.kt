package dev.fourco.xye.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fourco.xye.engine.model.InputIntent

@Composable
fun DPad(
    onInput: (InputIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonSize = 56.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Up
        FilledTonalButton(
            onClick = { onInput(InputIntent.MoveUp) },
            modifier = Modifier.size(buttonSize),
        ) {
            Text("\u25B2", fontSize = 20.sp)
        }

        // Left - Center - Right
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                onClick = { onInput(InputIntent.MoveLeft) },
                modifier = Modifier.size(buttonSize),
            ) {
                Text("\u25C0", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.size(buttonSize))

            FilledTonalButton(
                onClick = { onInput(InputIntent.MoveRight) },
                modifier = Modifier.size(buttonSize),
            ) {
                Text("\u25B6", fontSize = 20.sp)
            }
        }

        // Down
        FilledTonalButton(
            onClick = { onInput(InputIntent.MoveDown) },
            modifier = Modifier.size(buttonSize),
        ) {
            Text("\u25BC", fontSize = 20.sp)
        }
    }
}

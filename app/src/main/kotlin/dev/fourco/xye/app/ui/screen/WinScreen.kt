package dev.fourco.xye.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WinScreen(
    onHome: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F0E8))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Level Complete!",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF33CC33),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onReplay,
            modifier = Modifier.fillMaxWidth(0.6f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF44BBDD),
            ),
        ) {
            Text("Play Again", modifier = Modifier.padding(vertical = 8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth(0.6f),
        ) {
            Text("Home", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

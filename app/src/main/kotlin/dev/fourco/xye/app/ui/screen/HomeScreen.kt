package dev.fourco.xye.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onPlayDemo: () -> Unit,
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
            text = "Xye",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF33CC33),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "A puzzle game",
            fontSize = 18.sp,
            color = Color(0xFF8A7A5A),
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onPlayDemo,
            modifier = Modifier.fillMaxWidth(0.6f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF33CC33),
            ),
        ) {
            Text(
                text = "Play",
                fontSize = 18.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "473 levels from 28 community packs",
            fontSize = 14.sp,
            color = Color(0xFF8A7A5A),
            textAlign = TextAlign.Center,
        )
    }
}

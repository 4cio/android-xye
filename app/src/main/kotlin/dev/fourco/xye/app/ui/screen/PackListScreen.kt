package dev.fourco.xye.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fourco.xye.content.LevelPack

@Composable
fun PackListScreen(
    packs: List<LevelPack>,
    onPackSelected: (LevelPack) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F0E8)),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE8E0D0))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text("\u2190 Back", color = Color(0xFF4A3728))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Level Packs",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A3728),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${packs.size} packs",
                fontSize = 14.sp,
                color = Color(0xFF8A7A5A),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(packs) { pack ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPackSelected(pack) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pack.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4A3728),
                            )
                            if (pack.author.isNotEmpty()) {
                                Text(
                                    text = "by ${pack.author}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8A7A5A),
                                )
                            }
                        }
                        Text(
                            text = "${pack.levels.size} levels",
                            fontSize = 14.sp,
                            color = Color(0xFF33CC33),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

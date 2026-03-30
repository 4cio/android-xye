package dev.fourco.xye.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.fourco.xye.app.ui.screen.*
import dev.fourco.xye.app.ui.theme.XyeTheme
import dev.fourco.xye.app.viewmodel.GameViewModel
import dev.fourco.xye.content.KyeParser
import dev.fourco.xye.content.LevelPack
import dev.fourco.xye.engine.model.GameStatus

class MainActivity : ComponentActivity() {

    private val kyeParser = KyeParser()
    private var packs: List<LevelPack> = emptyList()
    private var currentViewModel: GameViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load all bundled .kye packs
        packs = loadBundledPacks()

        setContent {
            XyeTheme {
                var screen by remember { mutableStateOf("home") }
                var selectedPack by remember { mutableStateOf<LevelPack?>(null) }
                var selectedLevelIndex by remember { mutableStateOf(0) }

                val vm = currentViewModel
                val gameState = vm?.state?.collectAsState()
                val state = gameState?.value

                // Auto-navigate to win screen
                if (state?.status == GameStatus.Won && screen == "game") {
                    screen = "win"
                }

                when (screen) {
                    "home" -> HomeScreen(
                        onPlayDemo = { screen = "packs" },
                    )
                    "packs" -> PackListScreen(
                        packs = packs,
                        onPackSelected = { pack ->
                            selectedPack = pack
                            screen = "levels"
                        },
                        onBack = { screen = "home" },
                    )
                    "levels" -> {
                        val pack = selectedPack
                        if (pack != null) {
                            LevelSelectScreen(
                                pack = pack,
                                onLevelSelected = { index ->
                                    selectedLevelIndex = index
                                    launchLevel(pack, index)
                                    screen = "game"
                                },
                                onBack = { screen = "packs" },
                            )
                        }
                    }
                    "game" -> {
                        vm?.let { GameScreen(viewModel = it) }
                    }
                    "win" -> WinScreen(
                        onHome = { screen = "home" },
                        onReplay = {
                            selectedPack?.let { launchLevel(it, selectedLevelIndex) }
                            screen = "game"
                        },
                    )
                }
            }
        }
    }

    private fun launchLevel(pack: LevelPack, index: Int) {
        val stream = javaClass.classLoader?.getResourceAsStream("packs/kye/${findKyeFile(pack.id)}")
            ?: return
        val level = kyeParser.parseLevel(stream, index)
        currentViewModel = GameViewModel(level.toInitialState())
    }

    private fun findKyeFile(packId: String): String {
        // packId matches the filename we used to parse
        return "$packId.kye"
    }

    private fun loadBundledPacks(): List<LevelPack> {
        val kyeFiles = listOf(
            "2Fun4Me", "7Tasks", "9", "Action2", "alphabatch", "AntKye2",
            "Beginner", "Training", "RComb", "Sampler",
            "afebrile", "afebrile2", "afebrile3", "alanskye", "anoder",
            "blaster", "copy", "crowds", "crux4", "danish",
            "easy", "gary", "gsmick", "happy", "hexufo",
            "home", "hordes", "Hweyards", "InARush", "jg",
            "nelsons", "Newkye", "philsel1", "pong",
            "Ricardo", "vex", "vvv1", "vvv2", "vvv3", "xmas",
        )

        return kyeFiles.mapNotNull { name ->
            try {
                val stream = javaClass.classLoader?.getResourceAsStream("packs/kye/$name.kye")
                    ?: return@mapNotNull null
                kyeParser.parsePack(name, stream)
            } catch (e: Exception) {
                null // Skip unparseable packs
            }
        }.sortedBy { it.name.lowercase() }
    }
}

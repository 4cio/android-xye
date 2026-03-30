package dev.fourco.xye.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.fourco.xye.app.ui.screen.GameScreen
import dev.fourco.xye.app.ui.screen.HomeScreen
import dev.fourco.xye.app.ui.screen.WinScreen
import dev.fourco.xye.app.ui.theme.XyeTheme
import dev.fourco.xye.app.viewmodel.GameViewModel
import dev.fourco.xye.content.XsbParser
import dev.fourco.xye.engine.model.GameStatus

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels {
        val xsb = """
            ########
            #......#
            #.@.$..#
            #......#
            #......#
            ########
        """.trimIndent()
        val parser = XsbParser()
        val level = parser.parseLevel(xsb.byteInputStream(), 0)
        GameViewModel.Factory(level.toInitialState())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XyeTheme {
                var screen by remember { mutableStateOf("home") }
                val state by viewModel.state.collectAsState()

                // Auto-navigate to win screen
                if (state.status == GameStatus.Won && screen == "game") {
                    screen = "win"
                }

                when (screen) {
                    "home" -> HomeScreen(
                        onPlayDemo = { screen = "game" },
                    )
                    "game" -> GameScreen(viewModel = viewModel)
                    "win" -> WinScreen(
                        onHome = { screen = "home" },
                        onReplay = {
                            // Recreate ViewModel would require activity recreation
                            // For now just go back to game
                            screen = "game"
                        },
                    )
                }
            }
        }
    }
}

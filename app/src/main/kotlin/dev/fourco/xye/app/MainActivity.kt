package dev.fourco.xye.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import dev.fourco.xye.app.ui.screen.GameScreen
import dev.fourco.xye.app.viewmodel.GameViewModel
import dev.fourco.xye.content.XsbParser

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hardcoded Sokoban level for Phase 1
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
        val initialState = level.toInitialState()
        val viewModel = GameViewModel(initialState)

        setContent {
            MaterialTheme {
                GameScreen(viewModel = viewModel)
            }
        }
    }
}

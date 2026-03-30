package dev.fourco.xye.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import dev.fourco.xye.app.ui.screen.GameScreen
import dev.fourco.xye.app.viewmodel.GameViewModel
import dev.fourco.xye.content.XsbParser

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
            MaterialTheme {
                GameScreen(viewModel = viewModel)
            }
        }
    }
}

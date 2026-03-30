package dev.fourco.xye.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.fourco.xye.app.ui.screen.*

object Routes {
    const val HOME = "home"
    const val GAME = "game"
    const val WIN = "win"
}

@Composable
fun XyeNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onPlayDemo = { navController.navigate(Routes.GAME) },
            )
        }
        composable(Routes.GAME) {
            // GameScreen is wired via the Activity for now since ViewModel needs initial state
            // This will be properly wired when PackLoader is integrated with the UI
        }
        composable(Routes.WIN) {
            WinScreen(
                onHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onReplay = {
                    navController.popBackStack(Routes.GAME, inclusive = false)
                },
            )
        }
    }
}

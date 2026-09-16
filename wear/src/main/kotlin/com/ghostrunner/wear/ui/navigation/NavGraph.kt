package com.ghostrunner.wear.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.ghostrunner.wear.di.AppContainer
import com.ghostrunner.wear.ui.screens.ProfilePickerScreen
import com.ghostrunner.wear.ui.screens.RunControlsScreen

private object Routes {
    const val PICKER = "picker"
    const val RUN = "run/{profileId}"
    fun run(profileId: Long): String = "run/$profileId"
}

@Composable
fun GhostRunnerWearNavGraph(container: AppContainer) {
    val controller = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = controller,
        startDestination = Routes.PICKER,
    ) {
        composable(Routes.PICKER) {
            ProfilePickerScreen(
                repository = container.paceProfileRepository,
                onSelect = { id -> controller.navigate(Routes.run(id)) },
            )
        }
        composable(
            route = Routes.RUN,
            arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("profileId") ?: return@composable
            RunControlsScreen(profileId = id, container = container)
        }
    }
}

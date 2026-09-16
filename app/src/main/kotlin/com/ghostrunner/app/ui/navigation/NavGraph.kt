package com.ghostrunner.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ghostrunner.app.di.AppContainer
import com.ghostrunner.app.ui.screens.ProfileEditScreen
import com.ghostrunner.app.ui.screens.ProfileListScreen
import com.ghostrunner.app.ui.screens.RunScreen

object Routes {
    const val PROFILE_LIST = "profile-list"
    private const val PROFILE_EDIT = "profile-edit"
    const val PROFILE_EDIT_WITH_ID = "$PROFILE_EDIT?id={id}"
    const val RUN = "run/{profileId}"

    fun profileEdit(id: Long? = null): String =
        if (id == null) "$PROFILE_EDIT?id=" else "$PROFILE_EDIT?id=$id"

    fun run(profileId: Long): String = "run/$profileId"
}

@Composable
fun GhostRunnerNavGraph(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.PROFILE_LIST) {
        composable(Routes.PROFILE_LIST) {
            ProfileListScreen(
                repository = container.paceProfileRepository,
                onEditProfile = { id -> navController.navigate(Routes.profileEdit(id)) },
                onStartRun = { id -> navController.navigate(Routes.run(id)) },
            )
        }
        composable(
            route = Routes.PROFILE_EDIT_WITH_ID,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val rawId = entry.arguments?.getString("id")
            val profileId = rawId?.toLongOrNull()?.takeIf { it > 0L }
            ProfileEditScreen(
                repository = container.paceProfileRepository,
                profileId = profileId,
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.RUN,
            arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
        ) { entry ->
            val profileId = entry.arguments?.getLong("profileId") ?: return@composable
            RunScreen(
                profileId = profileId,
                container = container,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

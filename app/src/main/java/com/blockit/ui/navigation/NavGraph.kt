package com.blockit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.blockit.core.di.AppContainer
import com.blockit.ui.screens.apps.AppsPickerScreen
import com.blockit.ui.screens.apps.AppsPickerViewModel
import com.blockit.ui.screens.backup.BackupRestoreScreen
import com.blockit.ui.screens.backup.BackupViewModel
import com.blockit.ui.screens.guide.HowItWorksScreen
import com.blockit.ui.screens.guide.SetupGuideScreen
import com.blockit.ui.screens.insights.InsightsScreen
import com.blockit.ui.screens.insights.InsightsViewModel
import com.blockit.ui.screens.profiles.ProfileDetailScreen
import com.blockit.ui.screens.profiles.ProfileDetailViewModel
import com.blockit.ui.screens.profiles.ProfilesScreen
import com.blockit.ui.screens.profiles.ProfilesViewModel
import com.blockit.ui.screens.schedules.ScheduleEditorScreen
import com.blockit.ui.screens.settings.SettingsHubScreen
import com.blockit.ui.screens.strictmode.StrictModeScreen

object NavRoutes {
    const val PROFILES = "profiles"
    const val PROFILE_DETAIL = "profile/{profileId}"
    const val ADD_APPS = "profile/{profileId}/add_apps"
    const val SCHEDULES = "profile/{profileId}/schedules"
    const val SETTINGS_HUB = "settings"
    const val STRICT_MODE = "settings/strict_mode"
    const val SETUP_GUIDE = "settings/setup_guide"
    const val BACKUP_RESTORE = "settings/backup_restore"
    const val HOW_IT_WORKS = "settings/how_it_works"
    const val INSIGHTS = "insights"

    fun profileDetail(profileId: Long) = "profile/$profileId"
    fun addApps(profileId: Long) = "profile/$profileId/add_apps"
    fun schedules(profileId: Long) = "profile/$profileId/schedules"
}

@Composable
fun BlockItNavHost(
    navController: NavHostController,
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.PROFILES,
        modifier = modifier
    ) {
        composable(NavRoutes.PROFILES) {
            val viewModel: ProfilesViewModel = viewModel(
                factory = ProfilesViewModel.Factory(
                    container.profileRepository,
                    container.ruleEvaluator,
                    container.focusSessionManager,
                    container.preferenceManager
                )
            )
            ProfilesScreen(
                viewModel = viewModel,
                onProfileClick = { profileId ->
                    navController.navigate(NavRoutes.profileDetail(profileId))
                },
                onOpenSettingsClick = {
                    navController.navigate(NavRoutes.SETTINGS_HUB)
                },
                onOpenInsightsClick = {
                    navController.navigate(NavRoutes.INSIGHTS)
                }
            )
        }

        composable(
            route = NavRoutes.PROFILE_DETAIL,
            arguments = listOf(navArgument("profileId") { type = NavType.LongType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getLong("profileId") ?: 0L
            val viewModel: ProfileDetailViewModel = viewModel(
                factory = ProfileDetailViewModel.Factory(
                    profileId,
                    container.profileRepository,
                    container.ruleEvaluator
                )
            )
            ProfileDetailScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onAddAppsClick = { id ->
                    navController.navigate(NavRoutes.addApps(id))
                },
                onConfigureSchedulesClick = { id ->
                    navController.navigate(NavRoutes.schedules(id))
                }
            )
        }

        composable(
            route = NavRoutes.ADD_APPS,
            arguments = listOf(navArgument("profileId") { type = NavType.LongType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getLong("profileId") ?: 0L
            val viewModel: AppsPickerViewModel = viewModel(
                factory = AppsPickerViewModel.Factory(
                    profileId,
                    container.installedAppsRepository,
                    container.profileRepository,
                    container.ruleEvaluator
                )
            )
            AppsPickerScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.SCHEDULES,
            arguments = listOf(navArgument("profileId") { type = NavType.LongType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getLong("profileId") ?: 0L
            ScheduleEditorScreen(
                profileId = profileId,
                profileRepo = container.profileRepository,
                ruleEvaluator = container.ruleEvaluator,
                scheduleManager = container.scheduleManager,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS_HUB) {
            SettingsHubScreen(
                onNavigateToStrictMode = { navController.navigate(NavRoutes.STRICT_MODE) },
                onNavigateToSetupGuide = { navController.navigate(NavRoutes.SETUP_GUIDE) },
                onNavigateToBackupRestore = { navController.navigate(NavRoutes.BACKUP_RESTORE) },
                onNavigateToHowItWorks = { navController.navigate(NavRoutes.HOW_IT_WORKS) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.STRICT_MODE) {
            StrictModeScreen(
                preferenceManager = container.preferenceManager,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETUP_GUIDE) {
            SetupGuideScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.BACKUP_RESTORE) {
            val viewModel: BackupViewModel = viewModel(
                factory = BackupViewModel.Factory(
                    container.backupRepository
                )
            )
            BackupRestoreScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.HOW_IT_WORKS) {
            HowItWorksScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.INSIGHTS) {
            val viewModel: InsightsViewModel = viewModel(
                factory = InsightsViewModel.Factory(
                    container.usageInsightsRepository
                )
            )
            InsightsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

package com.blockit.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.blockit.ui.screens.dashboard.MainDashboardScreen
import com.blockit.ui.screens.dashboard.MainDashboardViewModel
import com.blockit.ui.screens.guide.HowItWorksScreen
import com.blockit.ui.screens.guide.SetupGuideScreen
import com.blockit.ui.screens.insights.InsightsScreen
import com.blockit.ui.screens.insights.InsightsViewModel
import com.blockit.ui.screens.onboarding.OnboardingScreen
import com.blockit.ui.screens.profiles.ProfileDetailScreen
import com.blockit.ui.screens.profiles.ProfileDetailViewModel
import com.blockit.ui.screens.profiles.ProfilesScreen
import com.blockit.ui.screens.profiles.ProfilesViewModel
import com.blockit.ui.screens.schedules.ScheduleEditorScreen
import com.blockit.ui.screens.settings.SettingsHubScreen
import com.blockit.ui.screens.strictmode.StrictModeScreen

object NavRoutes {
    const val ONBOARDING = "onboarding"
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
    val onboardingCompleted by container.preferenceManager.onboardingCompleted.collectAsState(initial = null)

    if (onboardingCompleted == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    val startDestination = if (onboardingCompleted == true) NavRoutes.PROFILES else NavRoutes.ONBOARDING

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
        popExitTransition = { fadeOut(animationSpec = tween(200)) },
        modifier = modifier.fillMaxSize().background(com.blockit.ui.theme.CreamBackground)
    ) {
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(
                preferenceManager = container.preferenceManager,
                onComplete = {
                    navController.navigate(NavRoutes.PROFILES) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(NavRoutes.PROFILES) {
            val viewModel: MainDashboardViewModel = viewModel(
                factory = MainDashboardViewModel.Factory(
                    container.profileRepository,
                    container.ruleEvaluator,
                    container.focusSessionManager,
                    container.scheduleManager,
                    container.usageInsightsRepository,
                    container.preferenceManager
                )
            )
            MainDashboardScreen(
                viewModel = viewModel,
                onAddAppsClick = { profileId ->
                    navController.navigate(NavRoutes.addApps(profileId))
                },
                onOpenSettingsClick = {
                    navController.navigate(NavRoutes.SETTINGS_HUB)
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
                onNavigateToOnboarding = { navController.navigate(NavRoutes.ONBOARDING) },
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

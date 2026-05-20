package com.mmids.ui.navigation

import android.content.Context
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mmids.MainActivity
import com.mmids.ui.screens.*

sealed class Screen(val route: String) {
    object Consent   : Screen("consent")
    object Dashboard : Screen("dashboard")
    object Logs      : Screen("logs")
    object Settings  : Screen("settings")
}

@Composable
fun MMIDSNavHost(activity: MainActivity) {
    val navController = rememberNavController()

    // Check if user has already consented
    val prefs = activity.getSharedPreferences("mmids_prefs", Context.MODE_PRIVATE)
    val hasConsented = prefs.getBoolean("user_consented", false)

    val startDest = if (hasConsented) Screen.Dashboard.route else Screen.Consent.route

    NavHost(navController = navController, startDestination = startDest) {

        // Consent — only shown on first launch
        composable(Screen.Consent.route) {
            ConsentScreen(
                onAgree = {
                    prefs.edit().putBoolean("user_consented", true).apply()
                    // Set default disguise to "MMID Default" alias (index 0)
                    prefs.edit().putInt("disguise_icon_index", 0).apply()
                    activity.applyDisguise()
                    activity.requestBatteryExemption()
                    activity.startStandbyService()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Consent.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateLogs     = { navController.navigate(Screen.Logs.route) },
                onNavigateSettings = { navController.navigate(Screen.Settings.route) },
                activity           = activity
            )
        }

        composable(Screen.Logs.route) {
            LogViewerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack   = { navController.popBackStack() },
                activity = activity
            )
        }
    }
}

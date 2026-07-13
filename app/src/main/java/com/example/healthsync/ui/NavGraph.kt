package com.example.healthsync.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.healthsync.ui.auth.AuthScreen
import com.example.healthsync.ui.dashboard.DashboardScreen
import com.example.healthsync.ui.onboarding.OnboardingScreen
import com.example.healthsync.ui.permissions.PermissionsScreen
import com.example.healthsync.ui.settings.SettingsScreen

object Routes {
    const val AUTH = "auth"
    const val ONBOARDING = "onboarding"
    const val PERMISSIONS = "permissions"
    const val SETTINGS = "settings"
    const val DASHBOARD = "dashboard"
}

@Composable
fun NavGraph(navController: NavHostController, startDestination: String = Routes.AUTH) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.AUTH) {
            AuthScreen(onLoginSuccess = {
                navController.navigate(Routes.ONBOARDING) {
                    popUpTo(Routes.AUTH) { inclusive = true }
                }
            })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onContinue = { navController.navigate(Routes.PERMISSIONS) })
        }
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(onContinue = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onContinue = { navController.navigate(Routes.DASHBOARD) },
                onLogout = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onLogout = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

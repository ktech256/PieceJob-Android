package com.piecejob.provider.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.piecejob.core.ui.navigation.Screen
import com.piecejob.provider.ui.dashboard.ProviderDashboardScreen // This will become HomeTab
import com.piecejob.provider.ui.jobs.ProviderJobsScreen
import com.piecejob.provider.ui.wallet.ProviderWalletTabScreen
import com.piecejob.provider.ui.messages.ProviderMessagesScreen
import com.piecejob.provider.ui.profile.ProviderProfileScreen

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomBarScreen(Screen.ProviderHome.route, "Home", Icons.Default.Home)
    object Jobs : BottomBarScreen(Screen.ProviderJobs.route, "Jobs", Icons.Default.Work)
    object Wallet : BottomBarScreen(Screen.ProviderWalletTab.route, "Wallet", Icons.Default.AccountBalanceWallet)
    object Messages : BottomBarScreen(Screen.ProviderMessages.route, "Messages", Icons.Default.Message)
    object Profile : BottomBarScreen(Screen.ProviderProfileTab.route, "Profile", Icons.Default.Person)
}

@Composable
fun ProviderMainScreen(
    onSosTrigger: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToSubScreen: (Screen) -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomBarScreen.Home,
        BottomBarScreen.Jobs,
        BottomBarScreen.Wallet,
        BottomBarScreen.Messages,
        BottomBarScreen.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomBarScreen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomBarScreen.Home.route) {
                ProviderDashboardScreen(onSosTrigger = onSosTrigger)
            }
            composable(BottomBarScreen.Jobs.route) {
                ProviderJobsScreen()
            }
            composable(BottomBarScreen.Wallet.route) {
                ProviderWalletTabScreen(onNavigate = onNavigateToSubScreen)
            }
            composable(BottomBarScreen.Messages.route) {
                ProviderMessagesScreen()
            }
            composable(BottomBarScreen.Profile.route) {
                ProviderProfileScreen(
                    onLogout = onLogout,
                    onNavigate = onNavigateToSubScreen
                )
            }
        }
    }
}

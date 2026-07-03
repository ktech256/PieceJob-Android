package com.piecejob.customer.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.piecejob.core.ui.navigation.Screen
import com.piecejob.customer.ui.dashboard.CustomerDashboardScreen
import com.piecejob.customer.ui.jobs.CustomerJobsScreen
import com.piecejob.customer.ui.wallet.CustomerWalletScreen
import com.piecejob.customer.ui.messages.CustomerMessagesScreen
import com.piecejob.customer.ui.account.CustomerAccountScreen

sealed class CustomerBottomBarScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : CustomerBottomBarScreen(Screen.CustomerHome.route, "Home", Icons.Default.Home)
    object Jobs : CustomerBottomBarScreen(Screen.CustomerJobs.route, "Jobs", Icons.Default.WorkOutline)
    object Wallet : CustomerBottomBarScreen(Screen.CustomerWalletTab.route, "Wallet", Icons.Default.AccountBalanceWallet)
    object Messages : CustomerBottomBarScreen(Screen.CustomerMessages.route, "Messages", Icons.Default.ChatBubbleOutline)
    object Account : CustomerBottomBarScreen(Screen.CustomerAccountTab.route, "Account", Icons.Default.PersonOutline)
}

@Composable
fun CustomerMainScreen(
    onLogout: () -> Unit,
    onNavigateToSubScreen: (String) -> Unit,
    viewModel: CustomerMainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { jobId ->
            onNavigateToSubScreen(Screen.CustomerTracking.passJobId(jobId))
        }
    }

    val items = listOf(
        CustomerBottomBarScreen.Home,
        CustomerBottomBarScreen.Jobs,
        CustomerBottomBarScreen.Wallet,
        CustomerBottomBarScreen.Messages,
        CustomerBottomBarScreen.Account
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
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
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CustomerBottomBarScreen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(CustomerBottomBarScreen.Home.route) {
                CustomerDashboardScreen(
                    onServiceClick = { service -> onNavigateToSubScreen(Screen.BookingFlow.passArgs(serviceCode = service.code)) },
                    onRequestServiceClick = { onNavigateToSubScreen(Screen.BookingFlow.route) },
                    onNavigateToBookingWithLocation = { location -> 
                        onNavigateToSubScreen(Screen.BookingFlow.passArgs(
                            address = location.address, 
                            lat = location.coordinates[1], 
                            lng = location.coordinates[0]
                        )) 
                    },
                    onProfileClick = { navController.navigate(CustomerBottomBarScreen.Account.route) },
                    onNotificationsClick = { onNavigateToSubScreen(Screen.CustomerNotifications.route) },
                    onSosClick = { onNavigateToSubScreen(Screen.ReportIssue.route) },
                    onNavigateToTracking = { jobId -> onNavigateToSubScreen(Screen.CustomerTracking.passJobId(jobId)) }
                )
            }
            composable(CustomerBottomBarScreen.Jobs.route) {
                CustomerJobsScreen(
                    onNavigateToJob = { route -> onNavigateToSubScreen(route) }
                )
            }
            composable(CustomerBottomBarScreen.Wallet.route) {
                CustomerWalletScreen()
            }
            composable(CustomerBottomBarScreen.Messages.route) {
                CustomerMessagesScreen(
                    onNavigateToChat = { jobId, otherUserId -> 
                        onNavigateToSubScreen(Screen.Chat.passArgs(jobId, otherUserId))
                    }
                )
            }
            composable(CustomerBottomBarScreen.Account.route) {
                CustomerAccountScreen(
                    onLogout = onLogout,
                    onNavigate = { screen -> onNavigateToSubScreen(screen.route) }
                )
            }
        }
    }
}

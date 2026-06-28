package com.piecejob.provider.ui.main

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.piecejob.core.ui.navigation.Screen
import com.piecejob.provider.ui.components.JobRequestBanner
import com.piecejob.provider.ui.dashboard.ProviderDashboardScreen
import com.piecejob.provider.ui.jobs.ProviderJobsScreen
import com.piecejob.provider.ui.messages.ProviderMessagesScreen
import com.piecejob.provider.ui.profile.ProviderProfileScreen
import com.piecejob.provider.ui.wallet.ProviderWalletTabScreen

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
    onNavigateToSubScreen: (String) -> Unit,
    viewModel: ProviderMainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val activeJobRequest by viewModel.notificationState.activeJobRequest.collectAsState()
    val isAccepting by viewModel.notificationState.isAccepting.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { jobId ->
            Log.d("NavGraphTrace", "Auto-navigating to tracking for job $jobId")
            onNavigateToSubScreen(Screen.ProviderTracking.passJobId(jobId))
        }
    }
    
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
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = BottomBarScreen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(BottomBarScreen.Home.route) {
                    ProviderDashboardScreen(
                        onSosTrigger = onSosTrigger,
                        onNavigateToTracking = { jobId -> onNavigateToSubScreen(Screen.ProviderTracking.passJobId(jobId)) }
                    )
                }
                composable(BottomBarScreen.Jobs.route) {
                    ProviderJobsScreen(
                        onNavigateToTracking = { jobId -> onNavigateToSubScreen(Screen.ProviderTracking.passJobId(jobId)) }
                    )
                }
                composable(BottomBarScreen.Wallet.route) {
                    ProviderWalletTabScreen(onNavigate = { onNavigateToSubScreen(it.route) })
                }
                composable(BottomBarScreen.Messages.route) {
                    ProviderMessagesScreen()
                }
                composable(BottomBarScreen.Profile.route) {
                    ProviderProfileScreen(
                        onLogout = onLogout,
                        onNavigate = { onNavigateToSubScreen(it.route) }
                    )
                }
            }

            // UBER-STYLE FOREGROUND BANNER
            AnimatedVisibility(
                visible = activeJobRequest != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).zIndex(99f)
            ) {
                activeJobRequest?.let { job ->
                    JobRequestBanner(
                        job = job,
                        isAccepting = isAccepting,
                        onAccept = { viewModel.acceptJob(it) },
                        onDecline = { viewModel.declineJob(it) }
                    )
                }
            }
        }
    }
}

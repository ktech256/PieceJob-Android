package com.piecejob.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.piecejob.BuildConfig
import com.piecejob.core.ui.auth.*
import com.piecejob.core.ui.onboarding.*
import com.piecejob.customer.ui.wallet.CustomerWalletScreen
import com.piecejob.customer.ui.tracking.CustomerTrackingScreen
import com.piecejob.customer.ui.tracking.JobTrackingViewModel
import com.piecejob.customer.ui.corporate.CorporateProfileScreen
import com.piecejob.customer.ui.support.ReportIssueScreen
import com.piecejob.provider.ui.dashboard.ProviderDashboardScreen
import com.piecejob.provider.ui.dashboard.ProviderDashboardViewModel
import com.piecejob.provider.ui.wallet.ProviderWalletScreen
import com.piecejob.provider.ui.verification.ProviderVerificationScreen
import com.piecejob.provider.ui.onboarding.DocumentUploadScreen
import com.piecejob.core.ui.referral.ReferralScreen
import com.piecejob.customer.ui.dashboard.CustomerDashboardScreen
import com.piecejob.core.ui.chat.ChatScreen
import com.piecejob.core.ui.analytics.ProviderAnalyticsScreen
import com.piecejob.core.ui.analytics.CustomerAnalyticsScreen
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    // Specification: Skip onboarding for authenticated users
    val startDest = if (authViewModel.isLoggedIn()) Screen.Dashboard.route else Screen.Welcome.route

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                onLoginClick = { navController.navigate(Screen.Login.route) },
                onRegisterClick = { navController.navigate(Screen.RegisterCountryLanguage.route) }
            )
        }

        composable(route = Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToOtp = { phoneNumber ->
                    navController.navigate(Screen.Otp.passPhoneNumber(phoneNumber))
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.RegisterCountryLanguage.route) {
            RegisterCountryLanguageScreen(
                viewModel = authViewModel,
                onNext = { navController.navigate(Screen.RegisterPhone.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.RegisterPhone.route) {
            RegisterPhoneScreen(
                viewModel = authViewModel,
                onNavigateToOtp = {
                    navController.navigate(Screen.Otp.passPhoneNumber(authViewModel.phoneNumber.value))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.Otp.route,
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            OtpScreen(
                phoneNumber = phoneNumber,
                viewModel = authViewModel,
                onOtpVerified = {
                    navController.navigate(Screen.RegistrationDetails.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.RegistrationDetails.route) {
            RegistrationDetailsScreen(
                viewModel = authViewModel,
                onSuccess = {
                    if (BuildConfig.FLAVOR == "provider") {
                        navController.navigate(Screen.ProviderServiceSelection.route)
                    } else {
                        // Customer flows directly to dashboard after registration
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.ProviderServiceSelection.route) {
            ProviderTradeSelectionScreen(
                authViewModel = authViewModel,
                onSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Dashboard.route) {
            if (BuildConfig.FLAVOR == "provider") {
                ProviderDashboardScreen(
                    onSosTrigger = { navController.navigate(Screen.ReportIssue.route) }
                )
            } else {
                CustomerDashboardScreen(
                    onServiceClick = { service ->
                        navController.navigate(Screen.CustomerTracking.passJobId("DEMO_JOB_123"))
                    },
                    onProfileClick = { navController.navigate(Screen.CustomerAnalytics.route) },
                    onNotificationsClick = { }
                )
            }
        }

        composable(route = Screen.CustomerWallet.route) { CustomerWalletScreen() }
        composable(route = Screen.CustomerAnalytics.route) { CustomerAnalyticsScreen() }
        
        composable(
            route = Screen.CustomerTracking.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            CustomerTrackingScreen(
                jobId = jobId,
                onChatOpen = { otherUserId ->
                    navController.navigate(Screen.Chat.passArgs(jobId, otherUserId))
                },
                onSosTrigger = { },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.CorporateProfile.route) { 
            CorporateProfileScreen(
                company = null,
                employees = emptyList(),
                schedules = emptyList()
            ) 
        }
        
        composable(route = Screen.ReportIssue.route) { 
            ReportIssueScreen(jobId = null, onSubmit = { _, _, _ -> }) 
        }
        
        composable(route = Screen.ProviderWallet.route) { 
            ProviderWalletScreen(onWithdrawClick = {}) 
        }
        
        composable(route = Screen.ProviderVerification.route) { 
            ProviderVerificationScreen(onUploadClick = {
                navController.navigate(Screen.DocumentUpload.route)
            }) 
        }

        composable(route = Screen.ProviderAnalytics.route) {
            ProviderAnalyticsScreen()
        }
        
        composable(route = Screen.DocumentUpload.route) { 
            DocumentUploadScreen(onUploadComplete = {
                navController.popBackStack()
            })
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            ChatScreen(
                jobId = jobId,
                otherUserId = otherUserId,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.Referral.route) { 
            ReferralScreen(referralCode = "REF123", onInviteClick = {}, themeColor = androidx.compose.ui.graphics.Color.Blue) 
        }
    }
}

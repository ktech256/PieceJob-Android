package com.piecejob.core.ui.navigation

import android.util.Log
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
import com.piecejob.customer.ui.corporate.CorporateProfileScreen
import com.piecejob.customer.ui.support.ReportIssueScreen
import com.piecejob.provider.ui.main.ProviderMainScreen
import com.piecejob.provider.ui.main.ProviderPlaceholderScreen
import com.piecejob.provider.ui.profile.*
import com.piecejob.provider.ui.verification.*
import com.piecejob.customer.ui.main.CustomerMainScreen
import com.piecejob.customer.ui.main.CustomerPlaceholderScreen
import com.piecejob.provider.ui.onboarding.DocumentUploadScreen
import com.piecejob.customer.ui.account.*
import com.piecejob.customer.ui.dashboard.CustomerDashboardScreen
import com.piecejob.core.ui.chat.ChatScreen
import com.piecejob.core.ui.chat.NegotiationScreen
import com.piecejob.core.ui.analytics.ProviderAnalyticsScreen
import com.piecejob.core.ui.analytics.CustomerAnalyticsScreen
import com.piecejob.provider.ui.tracking.ProviderTrackingScreen
import com.piecejob.core.ui.rating.RatingScreen
import com.piecejob.core.ui.communication.CallScreen
import com.piecejob.core.ui.communication.IncomingCallScreen
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.Button

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val TAG = "NavGraphTrace"
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
                    Log.d(TAG, "Login Success. Navigating to Dashboard.")
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
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route)
                    }
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
                    Log.d(TAG, "OTP Verified. Navigating to Personal Details.")
                    navController.navigate(Screen.RegistrationDetails.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.RegistrationDetails.route) {
            RegistrationDetailsScreen(
                viewModel = authViewModel,
                onSuccess = {
                    Log.d(TAG, "RegistrationDetails onSuccess triggered. Flavor: ${BuildConfig.FLAVOR}")
                    if (BuildConfig.FLAVOR == "provider") {
                        Log.d(TAG, "Navigating to ProviderServiceSelection")
                        navController.navigate(Screen.ProviderServiceSelection.route)
                    } else {
                        Log.d(TAG, "Navigating to Customer Dashboard")
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
                ProviderMainScreen(
                    onSosTrigger = { navController.navigate(Screen.ReportIssue.route) },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    },
                    onNavigateToSubScreen = { route ->
                        navController.navigate(route)
                    }
                )
            } else {
                CustomerMainScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    },
                    onNavigateToSubScreen = { route ->
                        navController.navigate(route)
                    }
                )
            }
        }

        // Customer Sub-screens
        composable(route = Screen.CustomerPersonalDetails.route) { PersonalDetailsScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerAddresses.route) { AddressesScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerSavedLocations.route) { SavedLocationsScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerPaymentMethods.route) { PaymentMethodsScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerWalletHub.route) { CustomerWalletScreen() }
        composable(route = Screen.CustomerInvoices.route) { CustomerWalletScreen() } // Wallet screen handles invoices in tabs
        composable(route = Screen.CustomerStatements.route) { StatementsScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerNotifications.route) { NotificationsScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerReferrals.route) { ReferralsScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerRewards.route) { RewardsScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerPlus.route) { PieceJobPlusScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerSosSettings.route) { SosSettingsScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerEmergencyContacts.route) { EmergencyContactsScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerPrivacy.route) { PrivacySettingsScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerSecurity.route) { SecurityScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerLanguage.route) { LanguageSelectionScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerCountry.route) { CountrySelectionScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerSupport.route) { SupportScreen(onBack = { navController.popBackStack() }) }
        composable(route = Screen.CustomerAbout.route) { AboutScreen(onBack = { navController.popBackStack() }) }
        
        composable(
            route = Screen.BookingFlow.route,
            arguments = listOf(
                navArgument("serviceCode") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("address") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = null }, // NavType doesn't have Double easily, use String
                navArgument("lng") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val serviceCode = backStackEntry.arguments?.getString("serviceCode")
            val address = backStackEntry.arguments?.getString("address")
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()

            com.piecejob.customer.ui.booking.BookingFlowScreen(
                initialServiceCode = serviceCode,
                initialAddress = address,
                initialLat = lat,
                initialLng = lng,
                onTrackingStart = { jobId ->
                    navController.navigate(Screen.CustomerTracking.passJobId(jobId)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Sub-screens for Provider
        composable(route = Screen.ProviderPersonalDetails.route) {
            ProviderPersonalDetailsScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.MyServices.route) {
            ProviderServicesScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.VerificationDocs.route) {
            ProviderVerificationScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.EquipmentTools.route) {
            ProviderEquipmentScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.Certifications.route) {
            ProviderCertificationsScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.Experience.route) {
            ProviderExperienceScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.WalletSettings.route) {
            ProviderWalletSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.BankDetails.route) {
            ProviderBankDetailsScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.Notifications.route) {
            ProviderNotificationsScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.Availability.route) {
            ProviderAvailabilityScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.Security.route) {
            ProviderSecurityScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.DeviceManagement.route) {
            ProviderDeviceScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.ProviderStatements.route) {
            ProviderStatementsScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.RecentTransactions.route) {
            com.piecejob.provider.ui.wallet.RecentTransactionsScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.Support.route) {
            ProviderSupportScreen(
                onBack = { navController.popBackStack() },
                onNavigateToTicket = { ticketId ->
                    navController.navigate(Screen.TicketDetail.passTicketId(ticketId))
                }
            )
        }
        composable(route = Screen.Reviews.route) {
            ProviderReviewsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.TicketDetail.route,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId") ?: ""
            ProviderTicketDetailScreen(
                ticketId = ticketId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Inbox.route) {
            ProviderInboxScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.Disputes.route) {
            ProviderDisputeScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.TermsPolicies.route) {
            ProviderTermsScreen(onBack = { navController.popBackStack() })
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
                    // Regular chat access from tracking (will be limited if in negotiation)
                    navController.navigate(Screen.Chat.passArgs(jobId, otherUserId))
                },
                onNegotiationOpen = { jId, providerId ->
                    navController.navigate(Screen.Negotiation.passArgs(jId, providerId))
                },
                onCallOpen = { receiverId, name, phone, photo ->
                    val route = Screen.Call.passArgs(jobId, receiverId, name, phone, photo)
                    Log.d("FORENSIC", "NAV_GRAPH | Navigating to Call Screen. Route: $route")
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                onSosTrigger = { },
                onNavigateToRating = {
                    navController.navigate(Screen.Rating.passJobId(jobId)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ProviderTracking.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            ProviderTrackingScreen(
                jobId = jobId,
                onChatOpen = { otherUserId ->
                    // Forensic Fix: Navigate to Chat instead of Negotiation.
                    // ChatScreen handles state-based locking and provides a link to Negotiation if needed.
                    navController.navigate(Screen.Chat.passArgs(jobId, otherUserId))
                },
                onCallOpen = { receiverId, name, phone, photo ->
                    val route = Screen.Call.passArgs(jobId, receiverId, name, phone, photo)
                    Log.d("FORENSIC", "NAV_GRAPH | Navigating to Call Screen. Route: $route")
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
                onNavigateToRating = {
                    navController.navigate(Screen.Rating.passJobId(jobId)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            )
        }

        composable(
            route = Screen.Rating.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            RatingScreen(
                jobId = jobId,
                onSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
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

        composable(route = Screen.ProviderAnalytics.route) {
            ProviderAnalyticsScreen()
        }
        
        composable(
            route = Screen.DocumentUpload.route,
            arguments = listOf(navArgument("docType") { type = NavType.StringType })
        ) { backStackEntry ->
            val docType = backStackEntry.arguments?.getString("docType") ?: ""
            DocumentUploadScreen(
                docType = docType,
                onUploadComplete = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Negotiation.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            NegotiationScreen(
                jobId = jobId,
                otherUserId = otherUserId,
                currentUserId = authViewModel.getUserId() ?: "",
                onBack = { navController.popBackStack() },
                onNegotiationComplete = { jId, _ ->
                    if (BuildConfig.FLAVOR == "provider") {
                        navController.navigate(Screen.ProviderTracking.passJobId(jId)) {
                            popUpTo(Screen.Negotiation.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.CustomerTracking.passJobId(jId)) {
                            popUpTo(Screen.Negotiation.route) { inclusive = true }
                        }
                    }
                }
            )
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
                onBack = { navController.popBackStack() },
                onNegotiationOpen = {
                    navController.navigate(Screen.Negotiation.passArgs(jobId, otherUserId)) {
                        popUpTo(Screen.Chat.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Call.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType },
                navArgument("receiverId") { type = NavType.StringType },
                navArgument("receiverName") { type = NavType.StringType },
                navArgument("receiverPhone") { type = NavType.StringType },
                navArgument("receiverPhoto") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
            val receiverName = backStackEntry.arguments?.getString("receiverName") ?: ""
            val receiverPhone = backStackEntry.arguments?.getString("receiverPhone") ?: ""
            val receiverPhoto = backStackEntry.arguments?.getString("receiverPhoto").let { if (it == "none") null else it }
            CallScreen(
                jobId = jobId,
                receiverId = receiverId,
                receiverName = receiverName,
                receiverPhone = receiverPhone,
                receiverPhoto = receiverPhoto,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.IncomingCall.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType },
                navArgument("callerId") { type = NavType.StringType },
                navArgument("callId") { type = NavType.StringType },
                navArgument("callerName") { type = NavType.StringType },
                navArgument("callerPhone") { type = NavType.StringType },
                navArgument("callerPhoto") { type = NavType.StringType },
                navArgument("autoAccept") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            val callerId = backStackEntry.arguments?.getString("callerId") ?: ""
            val callId = backStackEntry.arguments?.getString("callId") ?: ""
            val callerName = backStackEntry.arguments?.getString("callerName") ?: ""
            val callerPhone = backStackEntry.arguments?.getString("callerPhone") ?: ""
            val callerPhoto = backStackEntry.arguments?.getString("callerPhoto").let { if (it == "none") null else it }
            val autoAccept = backStackEntry.arguments?.getBoolean("autoAccept") ?: false
            
            IncomingCallScreen(
                jobId = jobId,
                callerId = callerId,
                callId = callId,
                callerName = callerName,
                callerPhone = callerPhone,
                callerPhoto = callerPhoto,
                autoAccept = autoAccept,
                onAccept = {
                    navController.navigate(Screen.Call.passArgs(jobId, callerId, callerName, callerPhone, callerPhoto)) {
                        popUpTo(Screen.IncomingCall.route) { inclusive = true }
                    }
                },
                onReject = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(route = Screen.Referral.route) {
            ProviderReferralScreen(onBack = { navController.popBackStack() })
        }
    }
}

package com.piecejob.core.ui.navigation

sealed class Screen(val route: String) {
    // Onboarding
    object Welcome : Screen("welcome")
    object RegisterCountryLanguage : Screen("register_country_language")
    object RegisterPhone : Screen("register_phone")
    object RegistrationDetails : Screen("registration_details")
    object ProviderServiceSelection : Screen("provider_service_selection")

    object Login : Screen("login")
    object Otp : Screen("otp/{phoneNumber}") {
        fun passPhoneNumber(phoneNumber: String) = "otp/$phoneNumber"
    }
    object Dashboard : Screen("dashboard")
    
    // Customer Screens
    object CustomerWallet : Screen("customer_wallet")
    object CustomerAnalytics : Screen("customer_analytics")
    object CustomerTracking : Screen("customer_tracking/{jobId}") {
        fun passJobId(jobId: String) = "customer_tracking/$jobId"
    }
    object CorporateProfile : Screen("corporate_profile")
    object ReportIssue : Screen("report_issue")
    
    // Provider Screens
    object ProviderDashboard : Screen("provider_dashboard")
    object ProviderWallet : Screen("provider_wallet")
    object ProviderVerification : Screen("provider_verification")
    object ProviderAnalytics : Screen("provider_analytics")
    object DocumentUpload : Screen("document_upload")
    
    // Common
    object Referral : Screen("referral")
    object Chat : Screen("chat/{jobId}/{otherUserId}") {
        fun passArgs(jobId: String, otherUserId: String) = "chat/$jobId/$otherUserId"
    }
}

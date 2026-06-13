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
    
    // Provider Tab Screens
    object ProviderHome : Screen("provider_home")
    object ProviderJobs : Screen("provider_jobs")
    object ProviderWalletTab : Screen("provider_wallet_tab")
    object ProviderMessages : Screen("provider_messages")
    object ProviderProfileTab : Screen("provider_profile_tab")

    // Provider Sub-Screens
    object MyServices : Screen("my_services")
    object VerificationDocs : Screen("verification_docs")
    object EquipmentTools : Screen("equipment_tools")
    object Certifications : Screen("certifications")
    object Experience : Screen("experience")
    object BankDetails : Screen("bank_details")
    object Notifications : Screen("notifications")
    object Security : Screen("security")
    object DeviceManagement : Screen("device_management")
    object Support : Screen("support")
    object Disputes : Screen("disputes")
    object TermsPolicies : Screen("terms_policies")
    object ProviderAnalytics : Screen("provider_analytics")
    object DocumentUpload : Screen("document_upload")
    
    // Common
    object Referral : Screen("referral")
    object Chat : Screen("chat/{jobId}/{otherUserId}") {
        fun passArgs(jobId: String, otherUserId: String) = "chat/$jobId/$otherUserId"
    }
}

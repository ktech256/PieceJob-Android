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
    
    // Customer Tab Screens
    object CustomerHome : Screen("customer_home")
    object CustomerJobs : Screen("customer_jobs")
    object CustomerWalletTab : Screen("customer_wallet_tab")
    object CustomerMessages : Screen("customer_messages")
    object CustomerAccountTab : Screen("customer_account_tab")

    // Customer Legacy/Sub Screens (referenced in NavGraph)
    object CustomerWallet : Screen("customer_wallet")
    object CustomerAnalytics : Screen("customer_analytics")
    
    // Customer Account Sub-Screens
    object CustomerPersonalDetails : Screen("customer_personal_details")
    object CustomerAddresses : Screen("customer_addresses")
    object CustomerSavedLocations : Screen("customer_saved_locations")
    object CustomerPaymentMethods : Screen("customer_payment_methods")
    object CustomerWalletHub : Screen("customer_wallet_hub")
    object CustomerInvoices : Screen("customer_invoices")
    object CustomerStatements : Screen("customer_statements")
    object CustomerNotifications : Screen("customer_notifications")
    object CustomerReferrals : Screen("customer_referrals")
    object CustomerRewards : Screen("customer_rewards")
    object CustomerPlus : Screen("customer_plus")
    object CustomerSosSettings : Screen("customer_sos_settings")
    object CustomerEmergencyContacts : Screen("customer_emergency_contacts")
    object CustomerPrivacy : Screen("customer_privacy")
    object CustomerSecurity : Screen("customer_security")
    object CustomerLanguage : Screen("customer_language")
    object CustomerCountry : Screen("customer_country")
    object CustomerSupport : Screen("customer_support")
    object CustomerAbout : Screen("customer_about")

    // Booking Flow
    object BookingFlow : Screen("booking_flow")
    object CustomerTracking : Screen("customer_tracking/{jobId}") {
        fun passJobId(jobId: String) = "customer_tracking/$jobId"
    }
    object ProviderTracking : Screen("provider_tracking/{jobId}") {
        fun passJobId(jobId: String) = "provider_tracking/$jobId"
    }
    object CorporateProfile : Screen("corporate_profile")
    object ReportIssue : Screen("report_issue")
    
    // Provider Tab Screens
    object ProviderHome : Screen("provider_home")
    object ProviderJobs : Screen("provider_jobs")
    object ProviderWalletTab : Screen("provider_wallet_tab")
    object ProviderMessages : Screen("provider_messages")
    object ProviderProfileTab : Screen("provider_profile_tab")

    // Provider Sub-Screens (Full Screen)
    object ProviderPersonalDetails : Screen("provider_personal_details")
    object MyServices : Screen("my_services")
    object VerificationDocs : Screen("verification_docs")
    object EquipmentTools : Screen("equipment_tools")
    object Certifications : Screen("certifications")
    object Experience : Screen("experience")
    object WalletSettings : Screen("wallet_settings")
    object BankDetails : Screen("bank_details")
    object Notifications : Screen("notifications")
    object Availability : Screen("availability")
    object Security : Screen("security")
    object DeviceManagement : Screen("device_management")
    object ProviderStatements : Screen("provider_statements")
    object Disputes : Screen("disputes")
    object Support : Screen("support")
    object Reviews : Screen("reviews")
    object TicketDetail : Screen("ticket_detail/{ticketId}") {
        fun passTicketId(ticketId: String) = "ticket_detail/$ticketId"
    }
    object Inbox : Screen("inbox")
    object TermsPolicies : Screen("terms_policies")
    object ProviderAnalytics : Screen("provider_analytics")
    object DocumentUpload : Screen("document_upload/{docType}") {
        fun passDocType(docType: String) = "document_upload/$docType"
    }
    
    // Provider Legacy/Other
    object ProviderDashboard : Screen("provider_dashboard")
    object ProviderWallet : Screen("provider_wallet")
    object ProviderVerification : Screen("provider_verification")

    // Common
    object Referral : Screen("referral")
    object Rating : Screen("rating/{jobId}") {
        fun passJobId(jobId: String) = "rating/$jobId"
    }
    object Chat : Screen("chat/{jobId}/{otherUserId}") {
        fun passArgs(jobId: String, otherUserId: String) = "chat/$jobId/$otherUserId"
    }
    object Call : Screen("call/{jobId}/{receiverId}/{receiverName}/{receiverPhone}") {
        fun passArgs(jobId: String, receiverId: String, receiverName: String, receiverPhone: String) = 
            "call/$jobId/$receiverId/$receiverName/$receiverPhone"
    }
    object IncomingCall : Screen("incoming_call/{jobId}/{callerId}/{callId}/{callerName}/{callerPhone}") {
        fun passArgs(jobId: String, callerId: String, callId: String, callerName: String, callerPhone: String) = 
            "incoming_call/$jobId/$callerId/$callId/$callerName/$callerPhone"
    }
}

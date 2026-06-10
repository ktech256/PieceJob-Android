package com.piecejob.core.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Otp : Screen("otp/{phoneNumber}") {
        fun passPhoneNumber(phoneNumber: String) = "otp/$phoneNumber"
    }
    object Dashboard : Screen("dashboard")
}

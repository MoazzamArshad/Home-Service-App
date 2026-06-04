package com.example.homeserve.ui.navigation

sealed class Screen(val route: String) {
    // Splash
    object Splash : Screen("splash")

    // Selection
    object Selection : Screen("selection")

    // Admin Screens
    object AdminLogin : Screen("admin_login")
    object AdminOtp : Screen("admin_otp/{phone}") {
        fun createRoute(phone: String) = "admin_otp/$phone"
    }
    object AdminDashboard : Screen("admin_dashboard")
    object CustomerManagement : Screen("customer_management")
    object ProviderApprovals : Screen("provider_approvals")
    object ProviderDetails : Screen("provider_details/{providerId}") {
        fun createRoute(providerId: String) = "provider_details/$providerId"
    }
    object ServiceCategories : Screen("service_categories")
    object BookingManagement : Screen("booking_management")
    object BookingDetails : Screen("booking_details/{bookingId}") {
        fun createRoute(bookingId: String) = "booking_details/$bookingId"
    }
    object Reports : Screen("reports")
    object AdminProfile : Screen("admin_profile")
    object AdminNotifications : Screen("admin_notifications")
    object AdminSecurity : Screen("admin_security")
    object AdminPrivacy : Screen("admin_privacy")
    object MoreMenu : Screen("more_menu")
    object Support : Screen("support")
    object Settings : Screen("settings")

    // Customer Screens
    object CustomerLogin : Screen("customer_login")
    object CustomerOtp : Screen("customer_otp/{phone}") {
        fun createRoute(phone: String) = "customer_otp/$phone"
    }
    object CustomerEmailLogin : Screen("customer_email_login")
    object CustomerProfileSetup : Screen("customer_profile_setup")
    object Home : Screen("home")
    object ServiceList : Screen("service_list/{categoryId}") {
        fun createRoute(categoryId: String) = "service_list/$categoryId"
    }
    object ServiceDetail : Screen("service_detail/{serviceId}") {
        fun createRoute(serviceId: String) = "service_detail/$serviceId"
    }
    object BookingAddress : Screen("booking_address/{serviceId}") {
        fun createRoute(serviceId: String) = "booking_address/$serviceId"
    }
    object BookingDetailsInput : Screen("booking_details_input/{serviceId}/{addressId}") {
        fun createRoute(serviceId: String, addressId: String) = "booking_details_input/$serviceId/$addressId"
    }
    object BookingConfirmation : Screen("booking_confirmation/{serviceId}/{addressId}") {
        fun createRoute(serviceId: String, addressId: String) = "booking_confirmation/$serviceId/$addressId"
    }
    object Bookings : Screen("bookings")
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object AddressManagement : Screen("address_management")
    object Rating : Screen("rating/{bookingId}") {
        fun createRoute(bookingId: String) = "rating/$bookingId"
    }

    // Provider Screens
    object ProviderLogin : Screen("provider_login")
    object ProviderOtp : Screen("provider_otp/{phone}") {
        fun createRoute(phone: String) = "provider_otp/$phone"
    }
    object ProviderEmailLogin : Screen("provider_email_login")
    object ProviderProfileSetup : Screen("provider_profile_setup")
    object ProviderCategorySelection : Screen("provider_category_selection")
    object ProviderServiceSelection : Screen("provider_service_selection/{categoryIds}") {
        fun createRoute(categoryIds: String) = "provider_service_selection/$categoryIds"
    }
    object ProviderHome : Screen("provider_home")
    object ProviderJobs : Screen("provider_jobs")
    object ProviderEarnings : Screen("provider_earnings")
    object ProviderProfile : Screen("provider_profile")
    object ProviderEditProfile : Screen("provider_edit_profile")
    object ProviderNotifications : Screen("provider_notifications")

    // Common
    object Chat : Screen("chat/{bookingId}/{senderRole}") {
        fun createRoute(bookingId: String, senderRole: String) = "chat/$bookingId/$senderRole"
    }
    object AiAssistant : Screen("ai_assistant")
    object ComingSoon : Screen("coming_soon/{title}") {
        fun createRoute(title: String) = "coming_soon/$title"
    }
}

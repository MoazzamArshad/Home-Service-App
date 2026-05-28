package com.example.homeserve.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.homeserve.ui.admin.AdminRootLayout
import com.example.homeserve.ui.admin.AdminRoute
import com.example.homeserve.ui.screens.*
import com.example.homeserve.ui.screens.admin.*
import com.example.homeserve.ui.screens.provider.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homeserve.ui.viewmodel.CustomerViewModel
import com.example.homeserve.ui.viewmodel.ProviderViewModel
import com.example.homeserve.ui.viewmodel.AdminViewModel
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.example.homeserve.ui.notifications.NotificationHelper

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("homeserve_prefs", Context.MODE_PRIVATE) }
    var hasAutoRedirected by remember { mutableStateOf(false) }

    val customerViewModel: CustomerViewModel = viewModel()
    val providerViewModel: ProviderViewModel = viewModel()
    val adminViewModel: AdminViewModel = viewModel()

    val navRoute by NotificationHelper.navigationRoute.collectAsState()
    LaunchedEffect(navRoute) {
        navRoute?.let { route ->
            try {
                navController.navigate(route) {
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            NotificationHelper.clearNavigation()
        }
    }

    LaunchedEffect(Unit) {
        val currentRole = sharedPrefs.getString("saved_role", null)
        val currentPhone = sharedPrefs.getString("saved_phone", null)
        if (currentRole != null && currentPhone != null) {
            when (currentRole) {
                "customer" -> {
                    if (currentPhone.contains("@")) {
                        customerViewModel.signInWithGoogle(currentPhone, "Customer") {}
                    } else {
                        customerViewModel.setCustomerId(currentPhone) {}
                    }
                }
                "provider" -> {
                    if (currentPhone.contains("@")) {
                        providerViewModel.signInWithGoogle(currentPhone, "Provider") {}
                    } else {
                        providerViewModel.setProviderId(currentPhone) {}
                    }
                }
                "admin" -> {
                    adminViewModel.setLoggedInAdmin(currentPhone, "Admin User")
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Selection.route,
        modifier = modifier
    ) {
        // --- Selection Screen ---
        composable(Screen.Selection.route) {
            // Commented out the 2-second auto-redirect state-management to allow clean manual role choice during presentations
            /*
            LaunchedEffect(Unit) {
                if (!hasAutoRedirected) {
                    val currentRole = sharedPrefs.getString("saved_role", null)
                    val currentPhone = sharedPrefs.getString("saved_phone", null)
                    if (currentRole != null && currentPhone != null) {
                        kotlinx.coroutines.delay(2000)
                        // Re-read preferences in case user clicked logout during the 2-second delay
                        val activeRole = sharedPrefs.getString("saved_role", null)
                        val activePhone = sharedPrefs.getString("saved_phone", null)
                        if (activeRole != null && activePhone != null) {
                            hasAutoRedirected = true
                            when (activeRole) {
                                "customer" -> {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Selection.route) { inclusive = true }
                                    }
                                }
                                "provider" -> {
                                    navController.navigate(Screen.ProviderHome.route) {
                                        popUpTo(Screen.Selection.route) { inclusive = true }
                                    }
                                }
                                "admin" -> {
                                    navController.navigate(Screen.AdminDashboard.route) {
                                        popUpTo(Screen.Selection.route) { inclusive = true }
                                    }
                                }
                            }
                        } else {
                            hasAutoRedirected = true
                        }
                    } else {
                        hasAutoRedirected = true
                    }
                }
            }
            */

            AppSelectionScreen(
                onRoleSelected = { role ->
                    when (role) {
                        UserRole.CUSTOMER -> navController.navigate(Screen.CustomerLogin.route)
                        UserRole.PROVIDER -> navController.navigate(Screen.ProviderLogin.route)
                        UserRole.ADMIN -> navController.navigate(Screen.AdminLogin.route)
                    }
                }
            )
        }

        // --- Provider Flow ---
        composable(Screen.ProviderLogin.route) {
            ProviderLoginScreen(
                onContinueClick = { phone ->
                    navController.navigate(Screen.ProviderOtp.createRoute(phone))
                },
                onGoogleSignInClick = { email, name ->
                    providerViewModel.signInWithGoogle(email, name) { exists ->
                        sharedPrefs.edit()
                            .putString("saved_role", "provider")
                            .putString("saved_phone", email)
                            .apply()
                        if (exists) {
                            navController.navigate(Screen.ProviderHome.route) {
                                popUpTo(Screen.Selection.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.ProviderProfileSetup.route) {
                                popUpTo(Screen.Selection.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(
            Screen.ProviderOtp.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            ProviderOtpScreen(
                phoneNumber = phone,
                onVerifyClick = {
                    providerViewModel.setProviderId(phone) { exists ->
                        sharedPrefs.edit()
                            .putString("saved_role", "provider")
                            .putString("saved_phone", phone)
                            .apply()
                        if (exists) {
                            navController.navigate(Screen.ProviderHome.route) {
                                popUpTo(Screen.Selection.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.ProviderProfileSetup.route)
                        }
                    }
                }
            )
        }

        composable(Screen.ProviderProfileSetup.route) {
            ProviderProfileSetupScreen(
                viewModel = providerViewModel,
                onContinueClick = {
                    navController.navigate(Screen.ProviderCategorySelection.route)
                }
            )
        }

        composable(Screen.ProviderCategorySelection.route) {
            ProviderCategorySelectionScreen(
                viewModel = providerViewModel,
                onContinueClick = { categories ->
                    navController.navigate(Screen.ProviderHome.route) {
                        popUpTo(Screen.Selection.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ProviderHome.route) {
            ProviderRootLayout(
                currentScreen = Screen.ProviderHome,
                onScreenSelected = { screen: Screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) {
                ProviderHomeScreen(
                    viewModel = providerViewModel,
                    onViewAllPendingClick = { navController.navigate(Screen.ProviderJobs.route) },
                    onNotificationBellClick = { navController.navigate(Screen.ProviderJobs.route) },
                    onReApplyClick = { navController.navigate(Screen.ProviderProfileSetup.route) }
                )
            }
        }

        composable(Screen.ProviderJobs.route) {
            ProviderRootLayout(
                currentScreen = Screen.ProviderJobs,
                onScreenSelected = { screen: Screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) {
                ProviderJobsScreen(
                    viewModel = providerViewModel,
                    onChatClick = { id -> navController.navigate(Screen.Chat.createRoute(id, "provider")) }
                )
            }
        }

        composable(Screen.ProviderEarnings.route) {
            ProviderRootLayout(
                currentScreen = Screen.ProviderEarnings,
                onScreenSelected = { screen: Screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) {
                ProviderEarningsScreen(
                    viewModel = providerViewModel
                )
            }
        }

        composable(Screen.ProviderProfile.route) {
            ProviderRootLayout(
                currentScreen = Screen.ProviderProfile,
                onScreenSelected = { screen: Screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) {
                ProviderProfileScreen(
                    viewModel = providerViewModel,
                    onLogoutClick = {
                        customerViewModel.stopListening()
                        providerViewModel.stopListening()
                        adminViewModel.stopListening()
                        sharedPrefs.edit().clear().apply()
                        navController.navigate(Screen.Selection.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSwitchToCustomerClick = {
                        val phone = providerViewModel.loggedInPhone
                        providerViewModel.stopListening()
                        sharedPrefs.edit()
                            .putString("saved_role", "customer")
                            .putString("saved_phone", phone)
                            .apply()
                        if (phone.contains("@")) {
                            customerViewModel.signInWithGoogle(phone, "Customer") {}
                        } else {
                            customerViewModel.setCustomerId(phone) {}
                        }
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onAvailabilityClick = {
                        navController.navigate(Screen.ComingSoon.createRoute("Availability & Schedule"))
                    },
                    onPayoutClick = {
                        navController.navigate(Screen.ComingSoon.createRoute("Payout Methods"))
                    },
                    onNotificationsClick = {
                        navController.navigate(Screen.ComingSoon.createRoute("Notification Preferences"))
                    },
                    onEditProfileClick = {
                        navController.navigate(Screen.ProviderEditProfile.route)
                    }
                )
            }
        }

        composable(Screen.ProviderEditProfile.route) {
            ProviderEditProfileScreen(
                viewModel = providerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- Admin Flow ---
        composable(Screen.AdminLogin.route) {
            AdminLoginScreen(
                viewModel = adminViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.AdminOtp.createRoute("Admin"))
                }
            )
        }

        composable(
            Screen.AdminOtp.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            OtpScreen(
                phoneNumber = phone,
                onBackClick = { navController.popBackStack() },
                onVerifyClick = {
                    sharedPrefs.edit()
                        .putString("saved_role", "admin")
                        .putString("saved_phone", adminViewModel.adminEmail.value)
                        .apply()
                    adminViewModel.completeAdminLogin()
                    navController.navigate(Screen.AdminDashboard.route) {
                        popUpTo(Screen.Selection.route) { inclusive = true }
                    }
                }
            )
        }

        // Admin screens with Bottom Bar
        val adminBottomBarScreens = listOf(
            Screen.AdminDashboard, Screen.CustomerManagement, Screen.ProviderApprovals,
            Screen.ServiceCategories, Screen.BookingManagement, Screen.Reports,
            Screen.AdminProfile, Screen.MoreMenu, Screen.Support, Screen.Settings
        )

        adminBottomBarScreens.forEach { screen ->
            composable(screen.route) {
                val adminRoute = when (screen) {
                    Screen.AdminDashboard -> AdminRoute.DASHBOARD
                    Screen.CustomerManagement -> AdminRoute.CUSTOMERS
                    Screen.ProviderApprovals -> AdminRoute.PROVIDERS
                    Screen.ServiceCategories -> AdminRoute.SERVICES
                    Screen.BookingManagement -> AdminRoute.BOOKINGS
                    Screen.Reports -> AdminRoute.REPORTS
                    Screen.AdminProfile -> AdminRoute.PROFILE
                    Screen.MoreMenu -> AdminRoute.MORE
                    Screen.Support -> AdminRoute.SUPPORT
                    Screen.Settings -> AdminRoute.SETTINGS
                    else -> AdminRoute.DASHBOARD
                }

                AdminRootLayout(
                    currentRoute = adminRoute,
                    onRouteSelected = { route ->
                        val target = when (route) {
                            AdminRoute.DASHBOARD -> Screen.AdminDashboard.route
                            AdminRoute.CUSTOMERS -> Screen.CustomerManagement.route
                            AdminRoute.PROVIDERS -> Screen.ProviderApprovals.route
                            AdminRoute.SERVICES -> Screen.ServiceCategories.route
                            AdminRoute.BOOKINGS -> Screen.BookingManagement.route
                            AdminRoute.REPORTS -> Screen.Reports.route
                            AdminRoute.PROFILE -> Screen.AdminProfile.route
                            AdminRoute.MORE -> Screen.MoreMenu.route
                            AdminRoute.SUPPORT -> Screen.Support.route
                            AdminRoute.SETTINGS -> Screen.Settings.route
                            else -> Screen.AdminDashboard.route
                        }
                        navController.navigate(target) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) { adminModifier ->
                    when (screen) {
                        Screen.AdminDashboard -> AdminDashboardScreen(
                            modifier = adminModifier,
                            viewModel = adminViewModel,
                            onNavigate = { path ->
                                when {
                                    path == "/admin/providers" -> navController.navigate(Screen.ProviderApprovals.route)
                                    path == "/admin/services" -> navController.navigate(Screen.ServiceCategories.route)
                                    path == "/admin/reports" -> navController.navigate(Screen.Reports.route)
                                    path == "/admin/support" -> navController.navigate(Screen.Support.route)
                                    path == "/admin/more" -> navController.navigate(Screen.MoreMenu.route)
                                    path.startsWith("/admin/bookings/") -> {
                                        val id = path.substringAfterLast("/")
                                        navController.navigate(Screen.BookingDetails.createRoute(id))
                                    }
                                }
                            }
                        )
                        Screen.CustomerManagement -> CustomerManagementScreen(
                            modifier = adminModifier,
                            viewModel = adminViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                        Screen.ProviderApprovals -> ProviderApprovalScreen(
                            modifier = adminModifier,
                            viewModel = adminViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onViewDetails = { id -> navController.navigate(Screen.ProviderDetails.createRoute(id)) }
                        )
                        Screen.ServiceCategories -> ServiceCategoryScreen(
                            modifier = adminModifier,
                            onNavigateBack = { navController.popBackStack() }
                        )
                        Screen.BookingManagement -> BookingManagementScreen(
                            modifier = adminModifier,
                            viewModel = adminViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetails = { id -> navController.navigate(Screen.BookingDetails.createRoute(id)) }
                        )
                        Screen.Reports -> ReportsScreen(
                            modifier = adminModifier,
                            viewModel = adminViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                        Screen.AdminProfile -> AdminProfileScreen(
                            viewModel = adminViewModel,
                            modifier = adminModifier,
                            onNavigate = { path ->
                                when (path) {
                                    "/admin/settings" -> navController.navigate(Screen.Settings.route)
                                    "/admin/notifications-settings" -> navController.navigate(Screen.AdminNotifications.route)
                                    "/admin/security" -> navController.navigate(Screen.AdminSecurity.route)
                                    "/admin/privacy" -> navController.navigate(Screen.AdminPrivacy.route)
                                    "/admin/about" -> navController.navigate(Screen.ComingSoon.createRoute("About Platform"))
                                }
                            },
                            onLogout = {
                                customerViewModel.stopListening()
                                providerViewModel.stopListening()
                                adminViewModel.stopListening()
                                sharedPrefs.edit().clear().apply()
                                navController.navigate(Screen.Selection.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                        Screen.MoreMenu -> MoreMenuScreen(
                            modifier = adminModifier,
                            onSupportClick = { navController.navigate(Screen.Support.route) },
                            onSettingsClick = { navController.navigate(Screen.Settings.route) },
                            onNavigate = { path ->
                                when (path) {
                                    "/admin/services" -> navController.navigate(Screen.ServiceCategories.route)
                                    "/admin/reports" -> navController.navigate(Screen.Reports.route)
                                    "/admin/providers" -> navController.navigate(Screen.ProviderApprovals.route)
                                }
                            },
                            onLogout = {
                                customerViewModel.stopListening()
                                providerViewModel.stopListening()
                                adminViewModel.stopListening()
                                sharedPrefs.edit().clear().apply()
                                navController.navigate(Screen.Selection.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                        Screen.Support -> SupportScreen(
                            modifier = adminModifier,
                            onNavigateBack = { navController.popBackStack() }
                        )
                        Screen.Settings -> SettingsScreen(
                            modifier = adminModifier,
                            onNavigateBack = { navController.popBackStack() },
                            onLogout = {
                                customerViewModel.stopListening()
                                providerViewModel.stopListening()
                                adminViewModel.stopListening()
                                sharedPrefs.edit().clear().apply()
                                navController.navigate(Screen.Selection.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onNavigate = { route -> navController.navigate(route) }
                        )
                        else -> {}
                    }
                }
            }
        }

        composable(
            Screen.ProviderDetails.route,
            arguments = listOf(navArgument("providerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
            ProviderDetailsScreen(
                viewModel = adminViewModel,
                providerId = providerId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.BookingDetails.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            BookingDetailsScreen(
                viewModel = adminViewModel,
                bookingId = bookingId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminNotifications.route) {
            ComingSoonScreen(title = "Notification Settings", onBackClick = { navController.popBackStack() })
        }

        composable(Screen.AdminSecurity.route) {
            ComingSoonScreen(title = "Security Settings", onBackClick = { navController.popBackStack() })
        }

        composable(Screen.AdminPrivacy.route) {
            ComingSoonScreen(title = "Privacy Policy", onBackClick = { navController.popBackStack() })
        }

        // --- Customer Flow ---
        composable(Screen.CustomerLogin.route) {
            LoginScreen(
                onContinueClick = { phone ->
                    navController.navigate(Screen.CustomerOtp.createRoute(phone))
                },
                onGoogleSignInClick = { email, name ->
                    customerViewModel.signInWithGoogle(email, name) { exists ->
                        sharedPrefs.edit()
                            .putString("saved_role", "customer")
                            .putString("saved_phone", email)
                            .apply()
                        if (exists) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Selection.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.CustomerProfileSetup.route) {
                                popUpTo(Screen.Selection.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(
            Screen.CustomerOtp.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            OtpScreen(
                phoneNumber = phone,
                onBackClick = { navController.popBackStack() },
                onVerifyClick = {
                    customerViewModel.setCustomerId(phone) { exists ->
                        sharedPrefs.edit()
                            .putString("saved_role", "customer")
                            .putString("saved_phone", phone)
                            .apply()
                        if (exists) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Selection.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.CustomerProfileSetup.route)
                        }
                    }
                }
            )
        }

        composable(Screen.CustomerProfileSetup.route) {
            CustomerProfileSetupScreen(
                onContinueClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.CustomerProfileSetup.route) { inclusive = true }
                    }
                },
                viewModel = customerViewModel
            )
        }

        composable(Screen.Home.route) {
            CustomerRootLayout(
                currentScreen = Screen.Home,
                onScreenSelected = { screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) {
                HomeScreen(
                    onCategoryClick = { id -> navController.navigate(Screen.ServiceList.createRoute(id)) },
                    onProfileClick = { navController.navigate(Screen.Profile.route) },
                    onAiAssistantClick = { navController.navigate(Screen.AiAssistant.route) },
                    viewModel = customerViewModel
                )
            }
        }

        composable(
            Screen.ServiceList.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            ServiceListScreen(
                categoryId = categoryId,
                onBackClick = { navController.popBackStack() },
                onServiceClick = { id -> navController.navigate(Screen.ServiceDetail.createRoute(id)) },
                viewModel = customerViewModel
            )
        }

        composable(
            Screen.ServiceDetail.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            ServiceDetailScreen(
                serviceId = serviceId,
                onBackClick = { navController.popBackStack() },
                onBookNowClick = { id -> navController.navigate(Screen.BookingDateTime.createRoute(id)) },
                viewModel = customerViewModel
            )
        }

        composable(
            Screen.BookingDateTime.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            BookingDateTimeScreen(
                onBackClick = { navController.popBackStack() },
                onContinueClick = { date, time ->
                    navController.navigate(Screen.BookingAddress.createRoute(serviceId, date, time))
                }
            )
        }

        composable(
            Screen.BookingAddress.route,
            arguments = listOf(
                navArgument("serviceId") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType },
                navArgument("time") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val time = backStackEntry.arguments?.getString("time") ?: ""
            BookingAddressScreen(
                onBackClick = { navController.popBackStack() },
                onAddAddressClick = { navController.navigate(Screen.AddressManagement.route) },
                onContinueClick = { addressId ->
                    navController.navigate(Screen.BookingConfirmation.createRoute(serviceId, date, time, addressId))
                }
            )
        }

        composable(
            Screen.BookingConfirmation.route,
            arguments = listOf(
                navArgument("serviceId") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType },
                navArgument("time") { type = NavType.StringType },
                navArgument("addressId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val time = backStackEntry.arguments?.getString("time") ?: ""
            val addressId = backStackEntry.arguments?.getString("addressId") ?: ""
            BookingConfirmationScreen(
                serviceId = serviceId,
                date = date,
                time = time,
                addressId = addressId,
                onViewBookingsClick = {
                    navController.navigate(Screen.Bookings.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBackToHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                viewModel = customerViewModel
            )
        }

        composable(Screen.Bookings.route) {
            CustomerRootLayout(
                currentScreen = Screen.Bookings,
                onScreenSelected = { screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) {
                BookingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onRateClick = { id -> navController.navigate(Screen.Rating.createRoute(id)) },
                    onChatClick = { id -> navController.navigate(Screen.Chat.createRoute(id, "customer")) },
                    viewModel = customerViewModel
                )
            }
        }

        composable(Screen.Notifications.route) {
            CustomerRootLayout(
                currentScreen = Screen.Notifications,
                onScreenSelected = { screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) {
                NotificationsScreen(onBackClick = { navController.popBackStack() })
            }
        }

        composable(Screen.Profile.route) {
            CustomerRootLayout(
                currentScreen = Screen.Profile,
                onScreenSelected = { screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            ) {
                ProfileScreen(
                    viewModel = customerViewModel,
                    onEditProfileClick = { navController.navigate(Screen.EditProfile.route) },
                    onAddressesClick = { navController.navigate(Screen.AddressManagement.route) },
                    onLogoutClick = {
                        customerViewModel.stopListening()
                        providerViewModel.stopListening()
                        adminViewModel.stopListening()
                        sharedPrefs.edit().clear().apply()
                        navController.navigate(Screen.Selection.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSwitchToProviderClick = { navController.navigate(Screen.ComingSoon.createRoute("Service Provider")) },
                    onBackClick = { navController.popBackStack() },
                    onPaymentMethodsClick = { navController.navigate(Screen.ComingSoon.createRoute("Payment Methods")) },
                    onSupportClick = { navController.navigate(Screen.ComingSoon.createRoute("Help & Support")) },
                    onTermsClick = { navController.navigate(Screen.ComingSoon.createRoute("Terms & Conditions")) },
                    onPrivacyClick = { navController.navigate(Screen.ComingSoon.createRoute("Privacy Policy")) }
                )
            }
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                viewModel = customerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.AddressManagement.route) {
            AddressManagementScreen(onBackClick = { navController.popBackStack() })
        }

        composable(
            Screen.Rating.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            RatingScreen(
                bookingId = bookingId,
                onBackClick = { navController.popBackStack() },
                onSubmitClick = { navController.popBackStack() }
            )
        }

        // --- Common ---
        composable(
            Screen.Chat.route,
            arguments = listOf(
                navArgument("bookingId") { type = NavType.StringType },
                navArgument("senderRole") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            val senderRole = backStackEntry.arguments?.getString("senderRole") ?: ""
            ChatScreen(
                bookingId = bookingId,
                senderRole = senderRole,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.AiAssistant.route) {
            AiAssistantScreen(
                onBackClick = { navController.popBackStack() },
                onBookCategoryClick = { categoryId ->
                    navController.navigate(Screen.ServiceList.createRoute(categoryId))
                }
            )
        }

        composable(
            Screen.ComingSoon.route,
            arguments = listOf(navArgument("title") { type = NavType.StringType })
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            ComingSoonScreen(
                title = title,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

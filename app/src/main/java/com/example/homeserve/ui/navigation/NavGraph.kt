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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.homeserve.ui.theme.BrandBlue
import androidx.compose.ui.unit.dp

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

    val isCustomerBlocked by customerViewModel.isBlockedEvent.collectAsState()
    LaunchedEffect(isCustomerBlocked) {
        if (isCustomerBlocked) {
            android.widget.Toast.makeText(context, "Your account has been blocked by the Administrator. Please contact support.", android.widget.Toast.LENGTH_LONG).show()
            navController.navigate(Screen.Selection.route) {
                popUpTo(0) { inclusive = true }
            }
            customerViewModel.clearBlockedEvent()
        }
    }

    val hasUnreadBookings by customerViewModel.hasUnreadBookings.collectAsState()
    val hasUnreadJobs by providerViewModel.hasUnreadJobs.collectAsState()

    val customerNotifications by customerViewModel.notifications.collectAsState()
    val providerNotifications by providerViewModel.notifications.collectAsState()

    val customerChatThreads by customerViewModel.chatThreads.collectAsState()
    val providerChatThreads by providerViewModel.chatThreads.collectAsState()

    val hasUnreadInbox = customerNotifications.any { !it.read } || customerChatThreads.any { it.unreadCount > 0 }

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

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        // --- Splash Screen ---
        composable(Screen.Splash.route) {
            SplashScreen(
                navController = navController,
                customerViewModel = customerViewModel,
                providerViewModel = providerViewModel,
                adminViewModel = adminViewModel
            )
        }

        // --- Selection Screen ---
        composable(Screen.Selection.route) {
            AppSelectionScreen(
                onRoleSelected = { role ->
                    val savedRole = sharedPrefs.getString("saved_role", null)
                    val savedPhone = sharedPrefs.getString("saved_phone", null)
                    when (role) {
                        UserRole.CUSTOMER -> {
                            if (savedRole == "customer" && savedPhone != null) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Selection.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.CustomerLogin.route)
                            }
                        }
                        UserRole.PROVIDER -> {
                            if (savedRole == "provider" && savedPhone != null) {
                                navController.navigate(Screen.ProviderHome.route) {
                                    popUpTo(Screen.Selection.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.ProviderLogin.route)
                            }
                        }
                        UserRole.ADMIN -> {
                            if (savedRole == "admin" && savedPhone != null) {
                                navController.navigate(Screen.AdminDashboard.route) {
                                    popUpTo(Screen.Selection.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.AdminLogin.route)
                            }
                        }
                    }
                }
            )
        }

        // --- Provider Flow ---
        composable(Screen.ProviderLogin.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? android.app.Activity
            var isLoading by remember { mutableStateOf(false) }

            ProviderLoginScreen(
                onContinueClick = { phone ->
                    if (activity != null) {
                        isLoading = true
                        providerViewModel.sendOtp(
                            phone = phone,
                            activity = activity,
                            onCodeSent = {
                                isLoading = false
                                navController.navigate(Screen.ProviderOtp.createRoute(phone))
                            },
                            onError = { err ->
                                isLoading = false
                                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        android.widget.Toast.makeText(context, "Could not initialize activity", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onEmailLoginClick = {
                    navController.navigate(Screen.ProviderEmailLogin.route)
                }
            )

            if (isLoading) {
                androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                }
            }
        }

        // --- Provider Email Login ---
        composable(Screen.ProviderEmailLogin.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? android.app.Activity
            val gso = remember {
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                ).requestEmail().build()
            }
            val googleSignInClient = remember {
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
            }
            val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                    val email = account.email ?: ""
                    val name = account.displayName ?: ""
                    providerViewModel.signInWithGoogle(email, name) { exists ->
                        sharedPrefs.edit().putString("saved_role", "provider").putString("saved_phone", email).apply()
                        if (exists) {
                            navController.navigate(Screen.ProviderHome.route) { popUpTo(Screen.Selection.route) { inclusive = true } }
                        } else {
                            navController.navigate(Screen.ProviderProfileSetup.route) { popUpTo(Screen.Selection.route) { inclusive = true } }
                        }
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Google sign-in failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
            ProviderEmailLoginScreen(
                onSignInSuccess = {
                    sharedPrefs.edit().putString("saved_role", "provider").putString("saved_phone", providerViewModel.loggedInPhone).apply()
                    if (providerViewModel.providerProfile.value != null) {
                        navController.navigate(Screen.ProviderHome.route) { popUpTo(Screen.Selection.route) { inclusive = true } }
                    } else {
                        navController.navigate(Screen.ProviderProfileSetup.route) { popUpTo(Screen.Selection.route) { inclusive = true } }
                    }
                },
                onRegisterSuccess = {
                    sharedPrefs.edit().putString("saved_role", "provider").putString("saved_phone", providerViewModel.loggedInPhone).apply()
                    navController.navigate(Screen.ProviderProfileSetup.route) { popUpTo(Screen.Selection.route) { inclusive = true } }
                },
                onBackClick = { navController.popBackStack() },
                onSignIn = { email, password, onResult -> providerViewModel.signInWithEmail(email, password, onResult) },
                onRegister = { name, email, password, onResult -> providerViewModel.registerWithEmail(name, email, password, onResult) },
                onForgotPassword = { email, onResult -> providerViewModel.sendPasswordResetEmail(email, onResult) },
                onGoogleSignInClick = {
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                }
            )
        }

        composable(
            Screen.ProviderOtp.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? android.app.Activity
            var isVerifying by remember { mutableStateOf(false) }

            ProviderOtpScreen(
                phoneNumber = phone,
                onVerifyClick = { code ->
                    isVerifying = true
                    providerViewModel.verifyOtp(
                        code = code,
                        onSuccess = {
                            providerViewModel.setProviderId(phone) { exists ->
                                isVerifying = false
                                sharedPrefs.edit()
                                    .putString("saved_role", "provider")
                                    .putString("saved_phone", phone)
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
                        },
                        onError = { err ->
                            isVerifying = false
                            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onResendClick = {
                    if (activity != null) {
                        providerViewModel.sendOtp(
                            phone = phone,
                            activity = activity,
                            onCodeSent = {
                                android.widget.Toast.makeText(context, "OTP code resent successfully", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            )

            if (isVerifying) {
                androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                }
            }
        }

        composable(Screen.ProviderProfileSetup.route) {
            ProviderProfileSetupScreen(
                viewModel = providerViewModel,
                onBackClick = {
                    providerViewModel.stopListening()
                    sharedPrefs.edit().clear().apply()
                    navController.navigate(Screen.Selection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onContinueClick = {
                    navController.navigate(Screen.ProviderCategorySelection.route)
                }
            )
        }

        composable(Screen.ProviderCategorySelection.route) {
            ProviderCategorySelectionScreen(
                viewModel = providerViewModel,
                onContinueClick = { categories ->
                    val categoriesString = categories.joinToString(",")
                    navController.navigate(Screen.ProviderServiceSelection.createRoute(categoriesString))
                }
            )
        }

        composable(
            route = Screen.ProviderServiceSelection.route,
            arguments = listOf(
                navArgument("categoryIds") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoriesString = backStackEntry.arguments?.getString("categoryIds") ?: ""
            val categoryIdsList = categoriesString.split(",").filter { it.isNotEmpty() }
            
            ProviderServiceSelectionScreen(
                viewModel = providerViewModel,
                categoryIds = categoryIdsList,
                onComplete = {
                    if (providerViewModel.providerProfile.value?.categoryId?.isNotEmpty() == true) {
                        navController.navigate(Screen.ProviderHome.route) {
                            popUpTo(Screen.ProviderHome.route) { inclusive = false }
                        }
                    } else {
                        navController.navigate(Screen.ProviderHome.route) {
                            popUpTo(Screen.Selection.route) { inclusive = true }
                        }
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
                },
                hasUnreadJobs = hasUnreadJobs
            ) {
                ProviderHomeScreen(
                    viewModel = providerViewModel,
                    onViewAllPendingClick = { navController.navigate(Screen.ProviderJobs.route) },
                    onNotificationBellClick = { navController.navigate(Screen.ProviderNotifications.route) },
                    onReApplyClick = { navController.navigate(Screen.ProviderProfileSetup.route) },
                    onDeleteAccountSuccess = {
                        sharedPrefs.edit().clear().apply()
                        navController.navigate(Screen.Selection.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.ProviderJobs.route) {
            LaunchedEffect(Unit) {
                providerViewModel.clearJobsUpdateBadge()
            }
            ProviderRootLayout(
                currentScreen = Screen.ProviderJobs,
                onScreenSelected = { screen: Screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                hasUnreadJobs = hasUnreadJobs
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
                },
                hasUnreadJobs = hasUnreadJobs
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
                },
                hasUnreadJobs = hasUnreadJobs
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
                    onCategoryServicesClick = {
                        navController.navigate(Screen.ProviderCategorySelection.route)
                    },
                    onNotificationsClick = {
                        navController.navigate(Screen.ComingSoon.createRoute("Notification Preferences"))
                    },
                    onEditProfileClick = {
                        navController.navigate(Screen.ProviderEditProfile.route)
                    },
                    onDeleteAccountSuccess = {
                        sharedPrefs.edit().clear().apply()
                        navController.navigate(Screen.Selection.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSupportClick = {
                        navController.navigate(Screen.ComingSoon.createRoute("Complaints & Support"))
                    }
                )
            }
        }

        composable(Screen.ProviderNotifications.route) {
            LaunchedEffect(Unit) {
                providerViewModel.startListeningToNotifications()
                providerViewModel.markAllNotificationsAsRead()
            }
            ProviderRootLayout(
                currentScreen = Screen.ProviderJobs,
                onScreenSelected = { screen: Screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                hasUnreadJobs = hasUnreadJobs
            ) {
                InboxScreen(
                    chatThreads = providerChatThreads,
                    notifications = providerNotifications,
                    onChatThreadClick = { thread ->
                        navController.navigate(Screen.Chat.createRoute(thread.bookingId, "provider"))
                    },
                    onNotificationClick = { notification ->
                        providerViewModel.markNotificationAsRead(notification.id)
                        if (notification.targetScreen.isNotEmpty()) {
                            try {
                                navController.navigate(notification.targetScreen)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    onDeleteNotification = { notification ->
                        providerViewModel.deleteNotification(notification.id)
                    },
                    onClearAllNotifications = {
                        providerViewModel.clearAllNotifications()
                    },
                    onDeleteChatThread = { thread ->
                        providerViewModel.deleteChatThread(thread.bookingId)
                    },
                    onBackClick = { navController.popBackStack() }
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
            val context = androidx.compose.ui.platform.LocalContext.current
            OtpScreen(
                phoneNumber = phone,
                onBackClick = { navController.popBackStack() },
                onVerifyClick = { enteredCode ->
                    sharedPrefs.edit()
                        .putString("saved_role", "admin")
                        .putString("saved_phone", adminViewModel.adminEmail.value)
                        .apply()
                    adminViewModel.completeAdminLogin()
                    navController.navigate(Screen.AdminDashboard.route) {
                        popUpTo(Screen.Selection.route) { inclusive = true }
                    }
                },
                onResendClick = {
                    android.widget.Toast.makeText(context, "Verification code resent successfully", android.widget.Toast.LENGTH_SHORT).show()
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
                                    path == "/admin/customers" -> navController.navigate(Screen.CustomerManagement.route)
                                    path == "/admin/bookings" -> navController.navigate(Screen.BookingManagement.route)
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
                            viewModel = adminViewModel,
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
                            viewModel = adminViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                        Screen.Settings -> SettingsScreen(
                            modifier = adminModifier,
                            viewModel = adminViewModel,
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
            AdminNotificationSettingsScreen(
                viewModel = adminViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminSecurity.route) {
            AdminSecuritySettingsScreen(
                viewModel = adminViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminPrivacy.route) {
            ComingSoonScreen(title = "Privacy Policy", onBackClick = { navController.popBackStack() })
        }

        // --- Customer Flow ---
        composable(Screen.CustomerLogin.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? android.app.Activity
            var isLoading by remember { mutableStateOf(false) }

            LoginScreen(
                onContinueClick = { phone ->
                    if (activity != null) {
                        isLoading = true
                        customerViewModel.sendOtp(
                            phone = phone,
                            activity = activity,
                            onCodeSent = {
                                isLoading = false
                                navController.navigate(Screen.CustomerOtp.createRoute(phone))
                            },
                            onError = { err ->
                                isLoading = false
                                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        android.widget.Toast.makeText(context, "Could not initialize activity", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onEmailLoginClick = {
                    navController.navigate(Screen.CustomerEmailLogin.route)
                }
            )

            if (isLoading) {
                androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                }
            }
        }

        // --- Customer Email Login ---
        composable(Screen.CustomerEmailLogin.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val gso = remember {
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                ).requestEmail().build()
            }
            val googleSignInClient = remember {
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
            }
            val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                    val email = account.email ?: ""
                    val name = account.displayName ?: ""
                    customerViewModel.signInWithGoogle(email, name) { exists ->
                        sharedPrefs.edit().putString("saved_role", "customer").putString("saved_phone", email).apply()
                        if (exists) {
                            navController.navigate(Screen.Home.route) { popUpTo(Screen.Selection.route) { inclusive = true } }
                        } else {
                            navController.navigate(Screen.CustomerProfileSetup.route) { popUpTo(Screen.Selection.route) { inclusive = true } }
                        }
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Google sign-in failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
            EmailLoginScreen(
                onSignInSuccess = {
                    sharedPrefs.edit().putString("saved_role", "customer").putString("saved_phone", customerViewModel.loggedInPhone).apply()
                    val profile = customerViewModel.userProfile.value
                    if (profile != null && profile.name.isNotBlank() && profile.phone.isNotBlank() && profile.address.isNotBlank()) {
                        navController.navigate(Screen.Home.route) { popUpTo(Screen.Selection.route) { inclusive = true } }
                    } else {
                        navController.navigate(Screen.CustomerProfileSetup.route) { popUpTo(Screen.Selection.route) { inclusive = true } }
                    }
                },
                onRegisterSuccess = {
                    sharedPrefs.edit().putString("saved_role", "customer").putString("saved_phone", customerViewModel.loggedInPhone).apply()
                    navController.navigate(Screen.CustomerProfileSetup.route) { popUpTo(Screen.Selection.route) { inclusive = true } }
                },
                onBackClick = { navController.popBackStack() },
                onSignIn = { email, password, onResult -> customerViewModel.signInWithEmail(email, password, onResult) },
                onRegister = { name, email, password, onResult -> customerViewModel.registerWithEmail(name, email, password, onResult) },
                onForgotPassword = { email, onResult -> customerViewModel.sendPasswordResetEmail(email, onResult) },
                onGoogleSignInClick = {
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                }
            )
        }

        composable(
            Screen.CustomerOtp.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? android.app.Activity
            var isVerifying by remember { mutableStateOf(false) }

            OtpScreen(
                phoneNumber = phone,
                onBackClick = { navController.popBackStack() },
                onVerifyClick = { code ->
                    isVerifying = true
                    customerViewModel.verifyOtp(
                        code = code,
                        onSuccess = {
                            customerViewModel.setCustomerId(phone) { exists ->
                                isVerifying = false
                                sharedPrefs.edit()
                                    .putString("saved_role", "customer")
                                    .putString("saved_phone", phone)
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
                        },
                        onError = { err ->
                            isVerifying = false
                            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onResendClick = {
                    if (activity != null) {
                        customerViewModel.sendOtp(
                            phone = phone,
                            activity = activity,
                            onCodeSent = {
                                android.widget.Toast.makeText(context, "OTP code resent successfully", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            )

            if (isVerifying) {
                androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                }
            }
        }

        composable(Screen.CustomerProfileSetup.route) {
            CustomerProfileSetupScreen(
                onBackClick = {
                    customerViewModel.stopListening()
                    sharedPrefs.edit().clear().apply()
                    navController.navigate(Screen.Selection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
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
                },
                hasUnreadBookings = hasUnreadBookings,
                hasUnreadInbox = hasUnreadInbox
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
                onBookNowClick = { id -> navController.navigate(Screen.BookingAddress.createRoute(id)) },
                viewModel = customerViewModel
            )
        }

        composable(
            Screen.BookingAddress.route,
            arguments = listOf(
                navArgument("serviceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            BookingAddressScreen(
                viewModel = customerViewModel,
                onBackClick = { navController.popBackStack() },
                onAddAddressClick = { navController.navigate(Screen.AddressManagement.route) },
                onContinueClick = { addressId ->
                    navController.navigate(Screen.BookingDetailsInput.createRoute(serviceId, addressId))
                }
            )
        }

        composable(
            Screen.BookingDetailsInput.route,
            arguments = listOf(
                navArgument("serviceId") { type = NavType.StringType },
                navArgument("addressId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            val addressId = backStackEntry.arguments?.getString("addressId") ?: ""
            BookingDetailsInputScreen(
                serviceId = serviceId,
                addressId = addressId,
                viewModel = customerViewModel,
                onBackClick = { navController.popBackStack() },
                onConfirmClick = {
                    navController.navigate(Screen.BookingConfirmation.createRoute(serviceId, addressId))
                }
            )
        }

        composable(
            Screen.BookingConfirmation.route,
            arguments = listOf(
                navArgument("serviceId") { type = NavType.StringType },
                navArgument("addressId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
            val addressId = backStackEntry.arguments?.getString("addressId") ?: ""
            BookingConfirmationScreen(
                serviceId = serviceId,
                addressId = addressId,
                viewModel = customerViewModel,
                onViewBookingsClick = {
                    navController.navigate(Screen.Bookings.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBackToHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Bookings.route) {
            LaunchedEffect(Unit) {
                customerViewModel.clearBookingsUpdateBadge()
            }
            CustomerRootLayout(
                currentScreen = Screen.Bookings,
                onScreenSelected = { screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                hasUnreadBookings = hasUnreadBookings,
                hasUnreadInbox = hasUnreadInbox
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
            LaunchedEffect(Unit) {
                customerViewModel.startListeningToNotifications()
                customerViewModel.markAllNotificationsAsRead()
            }
            CustomerRootLayout(
                currentScreen = Screen.Notifications,
                onScreenSelected = { screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                hasUnreadBookings = hasUnreadBookings,
                hasUnreadInbox = hasUnreadInbox
            ) {
                InboxScreen(
                    chatThreads = customerChatThreads,
                    notifications = customerNotifications,
                    onChatThreadClick = { thread ->
                        navController.navigate(Screen.Chat.createRoute(thread.bookingId, "customer"))
                    },
                    onNotificationClick = { notification ->
                        customerViewModel.markNotificationAsRead(notification.id)
                        if (notification.targetScreen.isNotEmpty()) {
                            try {
                                navController.navigate(notification.targetScreen)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    onDeleteNotification = { notification ->
                        customerViewModel.deleteNotification(notification.id)
                    },
                    onClearAllNotifications = {
                        customerViewModel.clearAllNotifications()
                    },
                    onDeleteChatThread = { thread ->
                        customerViewModel.deleteChatThread(thread.bookingId)
                    },
                    onBackClick = { navController.popBackStack() }
                )
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
                },
                hasUnreadBookings = hasUnreadBookings,
                hasUnreadInbox = hasUnreadInbox
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
                    onSwitchToProviderClick = {
                        val phone = customerViewModel.loggedInPhone
                        customerViewModel.stopListening()
                        sharedPrefs.edit()
                            .putString("saved_role", "provider")
                            .putString("saved_phone", phone)
                            .apply()
                        providerViewModel.setProviderId(phone) { exists ->
                            if (exists) {
                                navController.navigate(Screen.ProviderHome.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.ProviderProfileSetup.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    },
                    onBackClick = { navController.popBackStack() },
                    onNotificationsClick = { navController.navigate(Screen.ComingSoon.createRoute("Notification Settings")) },
                    onSupportClick = { navController.navigate(Screen.ComingSoon.createRoute("Complaints & Support")) },
                    onTermsClick = { navController.navigate(Screen.ComingSoon.createRoute("Terms & Conditions")) },
                    onPrivacyClick = { navController.navigate(Screen.ComingSoon.createRoute("Privacy Policy")) },
                    onDeleteAccountSuccess = {
                        sharedPrefs.edit().clear().apply()
                        navController.navigate(Screen.Selection.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
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
            AddressManagementScreen(
                viewModel = customerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            Screen.Rating.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            val bookingsList by customerViewModel.userBookings.collectAsState()
            val isLoading by customerViewModel.isLoading.collectAsState()
            val booking = remember(bookingId, bookingsList) { bookingsList.find { it.bookingId == bookingId } }
            val serviceName = booking?.serviceName ?: "Home Service"
            val context = LocalContext.current

            RatingScreen(
                bookingId = bookingId,
                serviceName = serviceName,
                isLoading = isLoading,
                onBackClick = { navController.popBackStack() },
                onSubmitClick = { ratingVal, reviewVal, tagsVal ->
                    customerViewModel.submitBookingReview(bookingId, ratingVal, reviewVal, tagsVal) { success ->
                        if (success) {
                            android.widget.Toast.makeText(context, "Thank you for your review! ⭐", android.widget.Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else {
                            android.widget.Toast.makeText(context, "Failed to submit review. Please try again.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
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
            LaunchedEffect(bookingId, senderRole) {
                if (senderRole == "customer") {
                    customerViewModel.markChatAsRead(bookingId)
                } else if (senderRole == "provider") {
                    providerViewModel.markChatAsRead(bookingId)
                }
            }
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

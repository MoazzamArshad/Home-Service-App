package com.example.homeserve.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.navigation.Screen
import com.example.homeserve.ui.viewmodel.CustomerViewModel
import com.example.homeserve.ui.viewmodel.ProviderViewModel
import com.example.homeserve.ui.viewmodel.AdminViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: androidx.navigation.NavHostController,
    customerViewModel: CustomerViewModel,
    providerViewModel: ProviderViewModel,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("homeserve_prefs", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        val currentRole = sharedPrefs.getString("saved_role", null)
        val currentPhone = sharedPrefs.getString("saved_phone", null)

        val startTime = System.currentTimeMillis()

        if (currentRole != null && currentPhone != null) {
            when (currentRole) {
                "customer" -> {
                    val onCheckComplete: (Boolean) -> Unit = { exists ->
                        val elapsed = System.currentTimeMillis() - startTime
                        val remainingDelay = maxOf(0L, 1200L - elapsed)
                        // Make sure we run the navigation on main thread after delay
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (exists) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.CustomerProfileSetup.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        }, remainingDelay)
                    }

                    if (currentPhone.contains("@")) {
                        customerViewModel.signInWithGoogle(currentPhone, "Customer", onCheckComplete)
                    } else {
                        customerViewModel.setCustomerId(currentPhone, onCheckComplete)
                    }
                }
                "provider" -> {
                    val onCheckComplete: (Boolean) -> Unit = { exists ->
                        val elapsed = System.currentTimeMillis() - startTime
                        val remainingDelay = maxOf(0L, 1200L - elapsed)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (exists) {
                                navController.navigate(Screen.ProviderHome.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.ProviderProfileSetup.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        }, remainingDelay)
                    }

                    if (currentPhone.contains("@")) {
                        providerViewModel.signInWithGoogle(currentPhone, "Provider", onCheckComplete)
                    } else {
                        providerViewModel.setProviderId(currentPhone, onCheckComplete)
                    }
                }
                "admin" -> {
                    adminViewModel.setLoggedInAdmin(currentPhone, "Admin User")
                    val elapsed = System.currentTimeMillis() - startTime
                    val remainingDelay = maxOf(0L, 1200L - elapsed)
                    delay(remainingDelay)
                    navController.navigate(Screen.AdminDashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
                else -> {
                    val elapsed = System.currentTimeMillis() - startTime
                    val remainingDelay = maxOf(0L, 1200L - elapsed)
                    delay(remainingDelay)
                    navController.navigate(Screen.Selection.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
        } else {
            val elapsed = System.currentTimeMillis() - startTime
            val remainingDelay = maxOf(0L, 1200L - elapsed)
            delay(remainingDelay)
            navController.navigate(Screen.Selection.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(110.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🏠", fontSize = 54.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "HomeServe",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your trusted home services platform",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(64.dp))
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

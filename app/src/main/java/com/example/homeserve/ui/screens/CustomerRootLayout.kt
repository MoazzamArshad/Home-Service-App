package com.example.homeserve.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.homeserve.ui.navigation.Screen
import com.example.homeserve.ui.theme.BrandBlue

@Composable
fun CustomerRootLayout(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            Surface(
                shadowElevation = 16.dp,
                color = Color.White,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(72.dp)
                ) {
                    val items = listOf(
                        NavigationItem("Home", Icons.Default.Home, Screen.Home),
                        NavigationItem("Bookings", Icons.AutoMirrored.Filled.Assignment, Screen.Bookings),
                        NavigationItem("Alerts", Icons.Default.Notifications, Screen.Notifications),
                        NavigationItem("Profile", Icons.Default.Person, Screen.Profile)
                    )

                    items.forEach { item ->
                        val isSelected = currentScreen.route == item.screen.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = isSelected,
                            onClick = { onScreenSelected(item.screen) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandBlue,
                                selectedTextColor = BrandBlue,
                                indicatorColor = Color(0xFFEFF6FF),
                                unselectedIconColor = Color(0xFF9CA3AF),
                                unselectedTextColor = Color(0xFF9CA3AF)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content()
        }
    }
}

private data class NavigationItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val screen: Screen
)

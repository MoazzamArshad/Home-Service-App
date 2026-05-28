package com.example.homeserve.ui.screens.provider

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.navigation.Screen

@Composable
fun ProviderRootLayout(
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
                        NavigationItem("Home", Icons.Outlined.Home, Icons.Filled.Home, Screen.ProviderHome),
                        NavigationItem("Jobs", Icons.AutoMirrored.Outlined.Assignment, Icons.AutoMirrored.Filled.Assignment, Screen.ProviderJobs),
                        NavigationItem("Earnings", Icons.Outlined.AttachMoney, Icons.Filled.AttachMoney, Screen.ProviderEarnings),
                        NavigationItem("Profile", Icons.Outlined.Person, Icons.Filled.Person, Screen.ProviderProfile)
                    )

                    items.forEach { item ->
                        val isSelected = currentScreen.route == item.screen.route
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon, 
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp)
                                ) 
                            },
                            label = { 
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                ) 
                            },
                            selected = isSelected,
                            onClick = { onScreenSelected(item.screen) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF2563EB),
                                selectedTextColor = Color(0xFF2563EB),
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
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
    val screen: Screen
)

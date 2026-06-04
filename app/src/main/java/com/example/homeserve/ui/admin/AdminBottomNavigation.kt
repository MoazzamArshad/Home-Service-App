package com.example.homeserve.ui.admin

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue

@Composable
fun AdminBottomNavigation(
    currentRoute: AdminRoute,
    onRouteSelected: (AdminRoute) -> Unit
) {
    val items = listOf(
        AdminNavigationItem("Home", Icons.Default.Dashboard, AdminRoute.DASHBOARD),
        AdminNavigationItem("Users", Icons.Default.People, AdminRoute.CUSTOMERS),
        AdminNavigationItem("Providers", Icons.Default.Engineering, AdminRoute.PROVIDERS),
        AdminNavigationItem("Bookings", Icons.AutoMirrored.Filled.Assignment, AdminRoute.BOOKINGS),
        AdminNavigationItem("More", Icons.Default.MoreHoriz, AdminRoute.MORE)
    )

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
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationBarItem(
                    icon = { 
                        Icon(
                            imageVector = item.icon, 
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
                    onClick = { onRouteSelected(item.route) },
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

private data class AdminNavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: AdminRoute
)

package com.example.homeserve.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.homeserve.ui.viewmodel.AdminViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue

data class DashboardStatItem(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val bgColor: Color,
    val iconColor: Color
)

data class DashboardQuickAction(
    val label: String,
    val icon: ImageVector,
    val path: String
)

@Composable
fun AdminDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel,
    onNavigate: (String) -> Unit
) {
    val totalUsers by viewModel.totalUsers.collectAsState()
    val totalProviders by viewModel.totalProviders.collectAsState()
    val totalBookings by viewModel.totalBookings.collectAsState()
    val totalRevenue by viewModel.totalRevenue.collectAsState()
    val recentBookings by viewModel.recentBookings.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    val stats = listOf(
        DashboardStatItem("Users", totalUsers.toString(), Icons.Default.Person, Color(0xFFEFF6FF), Color(0xFF2563EB)),
        DashboardStatItem("Providers", totalProviders.toString(), Icons.Default.Engineering, Color(0xFFECFDF5), Color(0xFF059669)),
        DashboardStatItem("Bookings", totalBookings.toString(), Icons.Default.Assignment, Color(0xFFF5F3FF), Color(0xFF7C3AED)),
        DashboardStatItem("Revenue", "Rs. $totalRevenue", Icons.Default.Payments, Color(0xFFFFFBEB), Color(0xFFD97706))
    )

    val quickActions = listOf(
        DashboardQuickAction("Provider Approvals", Icons.Default.VerifiedUser, "/admin/providers"),
        DashboardQuickAction("Service Categories", Icons.Default.Category, "/admin/services"),
        DashboardQuickAction("System Reports", Icons.Default.BarChart, "/admin/reports"),
        DashboardQuickAction("Support Tickets", Icons.Default.SupportAgent, "/admin/support")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Blue Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BrandBlue,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 48.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Admin Dashboard",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Platform overview and control",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { onNavigate("/admin/more") },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⚙️", fontSize = 20.sp)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Stats Cards
            Text(
                text = "Key Metrics",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stats.forEach { stat ->
                    StatCard(stat)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Quick Actions
            Text(
                text = "Quick Management",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                quickActions.chunked(2).forEach { rowActions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowActions.forEach { action ->
                            QuickActionCard(
                                action = action,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate(action.path) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recent Activity Section
            Text(
                text = "Recent Bookings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (recentBookings.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No recent bookings", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                recentBookings.forEach { booking ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { onNavigate("/admin/bookings/${booking.bookingId}") },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(BrandBlue, CircleShape))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Booking #${booking.bookingId.takeLast(4)} - ${booking.serviceName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF374151)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(stat: DashboardStatItem) {
    Card(
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = stat.bgColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(stat.icon, contentDescription = null, tint = stat.iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stat.value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111827)
            )
            Text(
                text = stat.label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun QuickActionCard(action: DashboardQuickAction, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color(0xFFF3F4F6)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(action.icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF374151),
                textAlign = TextAlign.Center
            )
        }
    }
}

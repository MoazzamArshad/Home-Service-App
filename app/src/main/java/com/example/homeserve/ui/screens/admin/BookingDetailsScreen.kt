package com.example.homeserve.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.AdminViewModel

@Composable
fun BookingDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel,
    bookingId: String,
    onNavigateBack: () -> Unit
) {
    val bookings by viewModel.bookingsList.collectAsState()
    val users by viewModel.usersList.collectAsState()
    val providers by viewModel.providersList.collectAsState()

    val booking = remember(bookings, bookingId) {
        bookings.find { it.bookingId == bookingId }
    }

    if (booking == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Booking not found", color = Color(0xFF6B7280))
        }
        return
    }

    val customer = remember(users, booking.userId) {
        users.find { it.uid == booking.userId }
    }

    val provider = remember(providers, booking.providerId) {
        providers.find { it.uid == booking.providerId }
    }

    val sdfDate = remember { java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()) }
    val sdfTime = remember { java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()) }
    val dateStr = remember(booking.scheduledDate) { sdfDate.format(booking.scheduledDate.toDate()) }
    val timeStr = remember(booking.scheduledDate) { sdfTime.format(booking.scheduledDate.toDate()) }

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
                .padding(top = 48.dp, bottom = 24.dp, start = 16.dp, end = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Booking Details",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "BKG-${booking.bookingId.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
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

            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Current Status", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                        Spacer(modifier = Modifier.height(4.dp))
                        BookingStatusBadgeLarge(status = booking.status)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Amount", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                        Text(
                            text = "Rs. ${booking.totalAmount}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = BrandBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Details Sections
            DetailSection(title = "Customer Information", icon = Icons.Default.Person) {
                DetailItem(label = "Name", value = customer?.name ?: "Customer (${booking.userId.takeLast(4)})")
                DetailItem(label = "Phone", value = customer?.phone ?: "N/A")
                DetailItem(label = "Address", value = booking.address)
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailSection(title = "Provider Information", icon = Icons.Default.Engineering) {
                DetailItem(label = "Assigned To", value = provider?.name ?: if (booking.providerId.isEmpty()) "Unassigned" else "Provider (${booking.providerId.takeLast(4)})")
                DetailItem(label = "Phone", value = provider?.phone ?: "N/A")
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailSection(title = "Service & Schedule", icon = Icons.AutoMirrored.Filled.Assignment) {
                DetailItem(label = "Service", value = booking.serviceName)
                DetailItem(label = "Date", value = dateStr)
                DetailItem(label = "Time Slot", value = timeStr)
                DetailItem(label = "Payment", value = booking.paymentStatus.replaceFirstChar { it.uppercase() })
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Color(0xFF374151))
                ) {
                    Text("Edit Booking", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626))
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DetailSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, fontWeight = FontWeight.Bold, color = Color(0xFF111827), fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF374151))
    }
}

@Composable
private fun BookingStatusBadgeLarge(status: String) {
    val (color, bgColor) = when (status.lowercase()) {
        "pending" -> Color(0xFFB45309) to Color(0xFFFFFBEB)
        "accepted" -> Color(0xFF1D4ED8) to Color(0xFFEFF6FF)
        "completed" -> Color(0xFF059669) to Color(0xFFECFDF5)
        "cancelled" -> Color(0xFFDC2626) to Color(0xFFFEF2F2)
        else -> Color(0xFF6B7280) to Color(0xFFF3F4F6)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

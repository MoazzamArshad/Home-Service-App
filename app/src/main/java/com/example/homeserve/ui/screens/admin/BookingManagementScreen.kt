package com.example.homeserve.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.AdminViewModel
import com.example.homeserve.data.model.Booking

@Composable
fun BookingManagementScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    val bookings by viewModel.bookingsList.collectAsState()
    val users by viewModel.usersList.collectAsState()
    val providers by viewModel.providersList.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("all") }

    val filters = listOf("all", "pending", "accepted", "completed", "cancelled")

    val filteredBookings = remember(bookings, users, providers, searchQuery, activeFilter) {
        bookings.filter { booking ->
            val customerName = users.find { it.uid == booking.userId }?.name ?: ""
            val providerName = providers.find { it.uid == booking.providerId }?.name ?: ""

            val matchesSearch = booking.bookingId.contains(searchQuery, ignoreCase = true) ||
                    customerName.contains(searchQuery, ignoreCase = true) ||
                    providerName.contains(searchQuery, ignoreCase = true) ||
                    booking.serviceName.contains(searchQuery, ignoreCase = true)
            val matchesFilter = activeFilter == "all" || booking.status.equals(activeFilter, ignoreCase = true)
            matchesSearch && matchesFilter
        }
    }

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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Booking Management",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Monitor all service requests",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Search Bar in Header
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by ID or name...", color = Color(0xFF9CA3AF)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = BrandBlue
                    )
                )
            }
        }

        // Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = activeFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { activeFilter = filter },
                    label = { Text(filter.replaceFirstChar { it.uppercase() }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBlue,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = Color(0xFF6B7280)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color(0xFFE5E7EB),
                        selectedBorderColor = BrandBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (filteredBookings.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No bookings found", color = Color(0xFF6B7280))
                    }
                }
            } else {
                items(filteredBookings) { booking ->
                    val customerName = remember(users, booking.userId) {
                        users.find { it.uid == booking.userId }?.name ?: "Customer (${booking.userId.takeLast(4)})"
                    }
                    val providerName = remember(providers, booking.providerId) {
                        providers.find { it.uid == booking.providerId }?.name ?: if (booking.providerId.isEmpty()) "Unassigned" else "Provider (${booking.providerId.takeLast(4)})"
                    }
                    val sdfDate = remember { java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault()) }
                    val sdfTime = remember { java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()) }
                    val dateStr = remember(booking.scheduledDate) { sdfDate.format(booking.scheduledDate.toDate()) }
                    val timeStr = remember(booking.scheduledDate) { sdfTime.format(booking.scheduledDate.toDate()) }

                    BookingManagementCard(
                        booking = booking,
                        customerName = customerName,
                        providerName = providerName,
                        dateStr = dateStr,
                        timeStr = timeStr,
                        onClick = { onNavigateToDetails(booking.bookingId) }
                    )
                }
            }
        }
    }
}

@Composable
fun BookingManagementCard(
    booking: Booking,
    customerName: String,
    providerName: String,
    dateStr: String,
    timeStr: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "BKG-${booking.bookingId.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = BrandBlue
                    )
                    Text(
                        text = booking.serviceName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111827)
                    )
                }
                BookingStatusBadge(status = booking.status)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Customer", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    Text(customerName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF374151))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Provider", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    Text(providerName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF374151))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$dateStr at $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = "Rs. ${booking.totalAmount}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111827)
                )
            }
        }
    }
}

@Composable
fun BookingStatusBadge(status: String) {
    val (color, bgColor) = when (status.lowercase()) {
        "pending" -> Color(0xFFB45309) to Color(0xFFFFFBEB)
        "accepted" -> Color(0xFF1D4ED8) to Color(0xFFEFF6FF)
        "completed" -> Color(0xFF059669) to Color(0xFFECFDF5)
        "cancelled" -> Color(0xFFDC2626) to Color(0xFFFEF2F2)
        else -> Color(0xFF6B7280) to Color(0xFFF3F4F6)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

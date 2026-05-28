package com.example.homeserve.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.data.CustomerMockData
import com.example.homeserve.ui.theme.BrandBlue
import java.text.SimpleDateFormat
import java.util.Locale

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homeserve.ui.viewmodel.CustomerViewModel
import com.example.homeserve.data.model.Booking
import com.example.homeserve.data.NetworkUtils

@Composable
fun BookingsScreen(
    onBackClick: () -> Unit,
    onRateClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerViewModel = viewModel()
) {
    var activeTab by remember { mutableStateOf("all") }
    val tabs = listOf("all", "pending", "accepted", "completed", "cancelled")
    val selectedIndex = tabs.indexOf(activeTab)
    
    val allBookings by viewModel.userBookings.collectAsState()
    val bookings = allBookings.filter { activeTab == "all" || it.status == activeTab }
    
    val allProvidersMap by viewModel.allProvidersMap.collectAsState()
    
    var bookingToCancel by remember { mutableStateOf<Booking?>(null) }
    var cancelReasonText by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.fetchUserBookings()
        viewModel.fetchAllProviders()
    }

    val context = LocalContext.current

    val currentBooking = bookingToCancel
    if (currentBooking != null) {
        if (currentBooking.status == "pending") {
            AlertDialog(
                onDismissRequest = { bookingToCancel = null },
                containerColor = Color.White,
                title = { Text("Cancel Booking", fontWeight = FontWeight.Bold, color = Color(0xFF111827)) },
                text = {
                    Text(
                        text = "Are you sure you want to cancel this booking request?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4B5563)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!NetworkUtils.isNetworkAvailable(context)) {
                                Toast.makeText(context, "No network connection. Please check your internet and try again.", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.cancelBooking(currentBooking.bookingId, "Cancelled by customer before acceptance")
                                bookingToCancel = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Yes, Cancel", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { bookingToCancel = null }
                    ) {
                        Text("No, Keep", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { 
                    bookingToCancel = null
                    cancelReasonText = ""
                },
                containerColor = Color.White,
                title = { Text("Cancel Booking", fontWeight = FontWeight.Bold, color = Color(0xFF111827)) },
                text = {
                    Column {
                        Text(
                            text = "Please tell us why you are cancelling this booking. Your feedback helps us improve our services.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4B5563)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = cancelReasonText,
                            onValueChange = { cancelReasonText = it },
                            placeholder = { Text("Enter reason for cancellation...", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF111827),
                                focusedContainerColor = Color(0xFFF3F4F6),
                                unfocusedContainerColor = Color(0xFFF3F4F6),
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = BrandBlue
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!NetworkUtils.isNetworkAvailable(context)) {
                                Toast.makeText(context, "No network connection. Please check your internet and try again.", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.cancelBooking(currentBooking.bookingId, cancelReasonText.trim())
                                bookingToCancel = null
                                cancelReasonText = ""
                            }
                        },
                        enabled = cancelReasonText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFF3F4F6),
                            disabledContentColor = Color(0xFF9CA3AF)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirm Cancellation", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            bookingToCancel = null
                            cancelReasonText = ""
                        }
                    ) {
                        Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                    }
                }
            )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "My Bookings",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Track and manage your service requests",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = Color.Transparent,
            contentColor = BrandBlue,
            edgePadding = 24.dp,
            divider = {},
            indicator = { tabPositions ->
                if (selectedIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = BrandBlue
                    )
                }
            }
        ) {
            tabs.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    text = {
                        Text(
                            text = tab.replaceFirstChar { it.uppercase() },
                            fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selectedContentColor = BrandBlue,
                    unselectedContentColor = Color(0xFF6B7280)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (bookings.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No bookings found in this category", color = Color(0xFF6B7280))
                    }
                }
            } else {
                items(bookings) { booking ->
                    BookingCard(
                        booking = booking,
                        allProvidersMap = allProvidersMap,
                        onRateClick = { onRateClick(booking.bookingId) },
                        onCompleteClick = {
                            if (!NetworkUtils.isNetworkAvailable(context)) {
                                Toast.makeText(context, "No network connection. Cannot mark as completed.", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.completeBooking(booking.bookingId)
                            }
                        },
                        onCancelClick = { bookingToCancel = booking },
                        onChatClick = { onChatClick(booking.bookingId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingCard(
    booking: com.example.homeserve.data.model.Booking,
    allProvidersMap: Map<String, com.example.homeserve.data.model.Provider>,
    onRateClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onCancelClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val context = LocalContext.current
    var providerPhotoBitmap by remember(booking.providerPhotoUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(booking.providerPhotoUrl) {
        if (booking.providerPhotoUrl.isNotBlank() && booking.providerPhotoUrl.startsWith("content://")) {
            try {
                val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(booking.providerPhotoUrl))
                providerPhotoBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val date = booking.scheduledDate.toDate()
    val formattedDate = dateFormat.format(date)
    val formattedTime = timeFormat.format(date)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = if (booking.serviceName.isNotBlank()) booking.serviceName else "Service Booking",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(status = booking.status)
                }
                Text(
                    text = "Rs. ${booking.totalAmount}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = BrandBlue
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$formattedDate at $formattedTime", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4B5563))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = booking.address, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4B5563), maxLines = 1)
            }

            if (booking.providerId.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color(0xFFEFF6FF)) {
                        Box(contentAlignment = Alignment.Center) {
                            val photoUrl = booking.providerPhotoUrl
                            if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
                                coil.compose.AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Provider Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else if (providerPhotoBitmap != null) {
                                Image(
                                    bitmap = providerPhotoBitmap!!.asImageBitmap(),
                                    contentDescription = "Provider Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text("👨‍🔧", fontSize = 16.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Service Provider", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B7280))
                        Text(
                            text = if (booking.providerName.isNotBlank()) booking.providerName else "Assigned Provider",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (booking.status == "accepted") {
                        if (booking.providerPhone.isNotBlank()) {
                            FilledTonalIconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${booking.providerPhone}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color(0xFFEFF6FF),
                                    contentColor = BrandBlue
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call Provider",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        FilledTonalIconButton(
                            onClick = onChatClick,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color(0xFFEFF6FF),
                                    contentColor = BrandBlue
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("💬", fontSize = 16.sp)
                        }
                    }
                }
            }

            if (booking.status == "pending") {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF3F4F6))
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onCancelClick,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626))
                ) {
                    Text("Cancel Booking", fontWeight = FontWeight.Bold)
                }
            }

            if (booking.status == "accepted") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626))
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onCompleteClick,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF2563EB))
                    ) {
                        Text("Complete", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (booking.status == "completed") {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRateClick,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF7ED), contentColor = Color(0xFFF59E0B))
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rate Service", fontWeight = FontWeight.Bold)
                }
            }

            if (booking.status == "cancelled" && booking.cancelReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Cancellation Reason:", 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                        color = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = booking.cancelReason, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color(0xFF991B1B)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (color, bgColor) = when (status) {
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

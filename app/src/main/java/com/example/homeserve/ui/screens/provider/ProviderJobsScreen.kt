package com.example.homeserve.ui.screens.provider

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.ProviderViewModel
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.homeserve.data.NetworkUtils

@Composable
fun ProviderJobsScreen(
    viewModel: ProviderViewModel,
    onChatClick: (String) -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.providerProfile.collectAsState()
    val incomingRequests by viewModel.incomingRequests.collectAsState()
    val activeJobs by viewModel.activeJobs.collectAsState()
    val completedJobs by viewModel.completedJobs.collectAsState()
    val cancelledJobs by viewModel.cancelledJobs.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProviderData()
    }

    var activeTab by remember { mutableStateOf("all") }
    val tabs = listOf("all", "pending", "accepted", "completed", "cancelled")
    val selectedIndex = tabs.indexOf(activeTab)

    val filteredJobs = when (activeTab) {
        "pending" -> incomingRequests
        "accepted" -> activeJobs
        "completed" -> completedJobs
        "cancelled" -> cancelledJobs
        else -> (incomingRequests + activeJobs + completedJobs + cancelledJobs).sortedByDescending { it.createdAt }
    }

    Column(
        modifier = Modifier
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
                Text(
                    text = "Job Requests",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You have ${filteredJobs.size} jobs listed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // Tab Row
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

        if (profile?.isAvailable == false) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Color(0xFFFEE2E2), // light red
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "You are currently offline. Please go online on the Home screen to accept job requests.",
                        color = Color(0xFF991B1B), // dark red
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (filteredJobs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No jobs found in this category.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredJobs) { job ->
                    if (job.status == "pending") {
                        JobRequestCard(
                            job = job,
                            currentProviderId = profile?.uid ?: "",
                            providerLatitude = profile?.providerLatitude ?: 0.0,
                            providerLongitude = profile?.providerLongitude ?: 0.0,
                            onAccept = { viewModel.acceptJob(job.bookingId) },
                            onDecline = { viewModel.declineJob(job.bookingId) },
                            isAcceptEnabled = profile?.isAvailable ?: true
                        )
                    } else {
                        AcceptedJobCard(
                            job = job,
                            providerLatitude = profile?.providerLatitude ?: 0.0,
                            providerLongitude = profile?.providerLongitude ?: 0.0,
                            onCancel = { viewModel.cancelJob(job.bookingId) },
                            onChatClick = { onChatClick(job.bookingId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JobRequestCard(
    job: com.example.homeserve.data.model.Booking,
    currentProviderId: String,
    providerLatitude: Double = 0.0,
    providerLongitude: Double = 0.0,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    isAcceptEnabled: Boolean = true
) {
    var showAcceptConfirmDialog by remember { mutableStateOf(false) }

    if (showAcceptConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showAcceptConfirmDialog = false },
            containerColor = Color.White,
            title = { Text("Accept Job Request", fontWeight = FontWeight.Bold, color = Color(0xFF111827)) },
            text = {
                Text(
                    text = "Are you sure you want to accept this booking request for Rs. ${job.totalAmount}? You will be assigned as the service provider immediately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4B5563)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAccept()
                        showAcceptConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Confirm Accept", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAcceptConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = job.serviceName, 
                            fontWeight = FontWeight.Bold, 
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFDC2626),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = Color.White,
                                fontSize = 8.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = if (job.customerName.isNotBlank()) job.customerName else "Customer #${job.userId.takeLast(4)}", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = Color(0xFF6B7280)
                    )
                }
                Surface(
                    color = Color(0xFFEFF6FF), 
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Rs. ${job.totalAmount}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📍", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = job.address, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color(0xFF6B7280)
                    )
                    if (providerLatitude != 0.0 && providerLongitude != 0.0 && job.customerLatitude != 0.0 && job.customerLongitude != 0.0) {
                        val distance = com.example.homeserve.data.LocationUtils.getDistanceInKm(
                            providerLatitude, providerLongitude,
                            job.customerLatitude, job.customerLongitude
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "📍 %.1f km away from you", distance),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandBlue
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📅", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Today at Flexible Time", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151))
                ) {
                    Text(text = "Decline", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { showAcceptConfirmDialog = true },
                    enabled = isAcceptEnabled,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAcceptEnabled) Color(0xFF2563EB) else Color(0xFF9CA3AF),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Accept Job",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AcceptedJobCard(
    job: com.example.homeserve.data.model.Booking,
    providerLatitude: Double = 0.0,
    providerLongitude: Double = 0.0,
    onCancel: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.serviceName, 
                        fontWeight = FontWeight.Bold, 
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = if (job.customerName.isNotBlank()) job.customerName else "Customer #${job.userId.takeLast(4)}", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = Color(0xFF6B7280)
                    )
                }
                Surface(
                    color = Color(0xFFEFF6FF), 
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = job.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color(0xFF2563EB),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📍", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = job.address, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color(0xFF6B7280)
                    )
                    if (providerLatitude != 0.0 && providerLongitude != 0.0 && job.customerLatitude != 0.0 && job.customerLongitude != 0.0) {
                        val distance = com.example.homeserve.data.LocationUtils.getDistanceInKm(
                            providerLatitude, providerLongitude,
                            job.customerLatitude, job.customerLongitude
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "📍 %.1f km away from you", distance),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandBlue
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📅", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Accepted / Ongoing", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color(0xFF6B7280)
                )
            }

            if (job.status == "accepted" || job.status == "in_progress") {
                Spacer(modifier = Modifier.height(16.dp))
                if (job.customerPhone.isNotBlank()) {
                    val context = LocalContext.current
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${job.customerPhone}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF2563EB))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call Customer",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Call", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Button(
                                onClick = onChatClick,
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF2563EB))
                            ) {
                                Text("💬", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chat", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626))
                        ) {
                            Text("Cancel Job", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onChatClick,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF2563EB))
                        ) {
                            Text("💬", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chat with Customer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626))
                        ) {
                            Text("Cancel Job", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            if (job.status == "cancelled" && job.cancelReason.isNotBlank()) {
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
                        text = job.cancelReason, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color(0xFF991B1B)
                    )
                }
            }
        }
    }
}

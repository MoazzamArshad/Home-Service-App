package com.example.homeserve.ui.screens.provider

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.ProviderViewModel
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.homeserve.data.NetworkUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationRequest
import android.os.Looper

@Composable
fun ProviderHomeScreen(
    viewModel: ProviderViewModel,
    onViewAllPendingClick: () -> Unit,
    onNotificationBellClick: () -> Unit,
    onReApplyClick: () -> Unit = {}
) {
    val profile by viewModel.providerProfile.collectAsState()
    val incomingRequests by viewModel.incomingRequests.collectAsState()
    val activeJobs by viewModel.activeJobs.collectAsState()
    val completedJobs by viewModel.completedJobs.collectAsState()

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(Unit) {
        viewModel.loadProviderData()
    }

    val isOnline = profile?.isAvailable ?: true
    val scrollState = rememberScrollState()
    
    // Header color animation based on status
    val headerColor by animateColorAsState(
        targetValue = if (isOnline) Color(0xFF2563EB) else Color(0xFF475569),
        animationSpec = tween(durationMillis = 500)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Dynamic Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = headerColor,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome Back!",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = profile?.name ?: "John Smith",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    
                    // Notification Icon with Badge
                    Box {
                        IconButton(
                            onClick = onNotificationBellClick,
                            modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White)
                        }
                        Surface(
                            modifier = Modifier.size(10.dp).align(Alignment.TopEnd),
                            color = Color(0xFFEF4444),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(2.dp, headerColor)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Online/Offline Status Card
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) Color(0xFF10B981) else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isOnline) "Status: Online" else "Status: Offline",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isOnline) "You are visible to customers" else "You won't receive new jobs",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Switch(
                            checked = isOnline,
                            onCheckedChange = { newValue ->
                                if (!NetworkUtils.isNetworkAvailable(context)) {
                                    Toast.makeText(context, "No network connection. Cannot change availability status.", Toast.LENGTH_LONG).show()
                                } else {
                                    viewModel.toggleAvailability(newValue)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            if (profile?.isApproved == false) {
                Spacer(modifier = Modifier.height(16.dp))
                if (profile?.isRejected == true) {
                    Surface(
                        color = Color(0xFFFEF2F2), // Light red/crimson
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)), // Red-300
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "❌",
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Verification Rejected",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B), // Red-800
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Your account verification has been rejected by administrators. Please review and update your profile details to re-apply.",
                                        color = Color(0xFFB91C1C), // Red-700
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onReApplyClick,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)
                            ) {
                                Text("Edit Profile & Re-apply", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Surface(
                        color = Color(0xFFFEF3C7), // Light amber
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)), // Amber
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Verification Pending",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E), // Dark brown/amber
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your account is currently under review by our administrators. You will be able to see and accept job requests once your profile has been approved.",
                                    color = Color(0xFFB45309), // Amber-700
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Statistics Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f), 
                    label = "Active Jobs", 
                    value = activeJobs.size.toString(), 
                    icon = "🕒", 
                    iconBg = Color(0xFFEFF6FF)
                )
                StatCard(
                    modifier = Modifier.weight(1f), 
                    label = "Earnings", 
                    value = "Rs. ${completedJobs.sumOf { it.totalAmount }}", 
                    icon = "💵", 
                    iconBg = Color(0xFFECFDF5)
                )
                StatCard(
                    modifier = Modifier.weight(1f), 
                    label = "Pending Requests", 
                    value = incomingRequests.size.toString(), 
                    icon = "📋", 
                    iconBg = Color(0xFFFFFBEB)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Incoming Requests", 
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = Color(0xFF111827)
                )
                TextButton(onClick = onViewAllPendingClick) {
                    Text(text = "See All", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (incomingRequests.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No new incoming requests", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                incomingRequests.take(2).forEach { request ->
                    PendingJobCard(
                        title = request.serviceName,
                        customer = "Customer #${request.userId.takeLast(4)}",
                        address = request.address,
                        date = "Today",
                        time = "Flexible",
                        price = "Rs. ${request.totalAmount}",
                        isNew = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Current Activity
            Text(
                text = "Ongoing Task", 
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val ongoingJob = activeJobs.firstOrNull()
            if (ongoingJob != null) {
                ActiveJobCard(
                    booking = ongoingJob
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No ongoing tasks", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, value: String, icon: String, iconBg: Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value, 
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                color = Color(0xFF111827)
            )
            Text(
                text = label, 
                style = MaterialTheme.typography.labelSmall, 
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
fun PendingJobCard(
    title: String, 
    customer: String, 
    address: String, 
    date: String, 
    time: String, 
    price: String,
    isNew: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title, 
                            fontWeight = FontWeight.Bold, 
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isNew) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFFEF4444),
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
                    }
                    Text(text = customer, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                }
                Text(
                    text = price, 
                    color = Color(0xFF2563EB), 
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📍", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = address, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🕒", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$date at $time", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
            }
        }
    }
}

@Composable
fun ActiveJobCard(
    booking: com.example.homeserve.data.model.Booking
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.size(44.dp).background(Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🛠️", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = booking.serviceName, 
                            fontWeight = FontWeight.Bold, 
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "Customer: #${booking.userId.takeLast(4)}", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color(0xFF6B7280)
                        )
                    }
                }
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = booking.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color(0xFF2563EB),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "📍 Address: ${booking.address}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4B5563)
            )
        }
    }
}

package com.example.homeserve.ui.screens.provider

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.ProviderViewModel
import com.example.homeserve.data.NetworkUtils
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

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

    var activeTab by remember { mutableStateOf("pending") }
    val tabs = listOf("pending", "accepted", "completed", "cancelled")
    val selectedIndex = tabs.indexOf(activeTab)

    var selectedJobDetails by remember { mutableStateOf<com.example.homeserve.data.model.Booking?>(null) }
    var zoomedPhotoUrlMain by remember { mutableStateOf<String?>(null) }
    var showPriceUpdateDialogForJob by remember { mutableStateOf<com.example.homeserve.data.model.Booking?>(null) }
    var listPriceInput by remember { mutableStateOf("") }
    var listPriceError by remember { mutableStateOf<String?>(null) }

    var currentReviewingJob by remember { mutableStateOf<com.example.homeserve.data.model.Booking?>(null) }

    val allAvailableJobs = incomingRequests + activeJobs + completedJobs + cancelledJobs
    val liveCurrentJob = remember(currentReviewingJob, allAvailableJobs) {
        val found = currentReviewingJob?.let { current ->
            allAvailableJobs.find { it.bookingId == current.bookingId }
        }
        android.util.Log.d("ProviderJobsScreen", "liveCurrentJob computed. currentReviewingJob=${currentReviewingJob?.bookingId}, found=${found?.bookingId}")
        found
    }

    LaunchedEffect(incomingRequests.size, activeJobs.size, liveCurrentJob == null) {
        val currentIsValid = liveCurrentJob != null && 
                (activeJobs.any { it.bookingId == liveCurrentJob.bookingId } || 
                 incomingRequests.any { it.bookingId == liveCurrentJob.bookingId })
                 
        android.util.Log.d("ProviderJobsScreen", "LaunchedEffect triggered. incomingRequests.size=${incomingRequests.size}, activeJobs.size=${activeJobs.size}, liveCurrentJob=${liveCurrentJob?.bookingId}, currentIsValid=$currentIsValid")
                 
        if (!currentIsValid) {
            if (activeJobs.isNotEmpty()) {
                currentReviewingJob = activeJobs.first()
                android.util.Log.d("ProviderJobsScreen", "Selected activeJobs.first(): ${currentReviewingJob?.bookingId}")
            } else if (incomingRequests.isNotEmpty()) {
                currentReviewingJob = incomingRequests.first()
                android.util.Log.d("ProviderJobsScreen", "Selected incomingRequests.first(): ${currentReviewingJob?.bookingId}")
            } else {
                currentReviewingJob = null
                android.util.Log.d("ProviderJobsScreen", "Selected null")
            }
        }
    }

    val filteredJobs = when (activeTab) {
        "pending" -> incomingRequests
        "accepted" -> activeJobs
        "completed" -> completedJobs
        "cancelled" -> cancelledJobs
        else -> emptyList()
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

        if (activeJobs.size >= 2) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Color(0xFFFEF3C7), // light amber
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ℹ️", modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "You have 2 ongoing active jobs. Please complete or cancel one before accepting another.",
                        color = Color(0xFF92400E), // dark amber
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
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
                                onAccept = { price -> viewModel.acceptJob(job.bookingId, price) },
                                onDecline = { viewModel.declineJob(job.bookingId) },
                                isAcceptEnabled = (profile?.isAvailable ?: true) && activeJobs.size < 2,
                                onPhotoClick = { zoomedPhotoUrlMain = it },
                                onClick = { 
                                    selectedJobDetails = job
                                }
                            )
                        } else {
                            AcceptedJobCard(
                                job = job,
                                providerLatitude = profile?.providerLatitude ?: 0.0,
                                providerLongitude = profile?.providerLongitude ?: 0.0,
                                onCancel = { viewModel.cancelJob(job.bookingId) },
                                onChatClick = { onChatClick(job.bookingId) },
                                onPhotoClick = { zoomedPhotoUrlMain = it },
                                onMarkPaymentReceived = { viewModel.markPaymentReceived(job.bookingId) },
                                onUpdatePriceClick = {
                                    listPriceInput = job.totalAmount.toString()
                                    listPriceError = null
                                    showPriceUpdateDialogForJob = job
                                },
                                onClick = { 
                                    selectedJobDetails = job
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedJobDetails?.let { detailJob ->
        // Retrieve dynamic status updates directly from live flows
        val liveJob = (incomingRequests + activeJobs + completedJobs + cancelledJobs).find { it.bookingId == detailJob.bookingId } ?: detailJob
        ProviderJobDetailsOverlay(
            job = liveJob,
            providerLatitude = profile?.providerLatitude ?: 0.0,
            providerLongitude = profile?.providerLongitude ?: 0.0,
            isAcceptEnabled = (profile?.isAvailable ?: true) && activeJobs.size < 2,
            onAccept = { price ->
                viewModel.acceptJob(liveJob.bookingId, price)
                selectedJobDetails = null
            },
            onDecline = { viewModel.declineJob(liveJob.bookingId) },
            onCancel = { viewModel.cancelJob(liveJob.bookingId) },
            onChatClick = { onChatClick(liveJob.bookingId) },
            onMarkPaymentReceived = { viewModel.markPaymentReceived(liveJob.bookingId) },
            onUpdatePrice = { price ->
                viewModel.updateBookingPrice(liveJob.bookingId, price)
            },
            onClose = { selectedJobDetails = null }
        )
    }

    if (zoomedPhotoUrlMain != null) {
        AlertDialog(
            onDismissRequest = { zoomedPhotoUrlMain = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black.copy(alpha = 0.9f),
            title = null,
            text = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = zoomedPhotoUrlMain,
                        contentDescription = "Zoomed problem photo",
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    IconButton(
                        onClick = { zoomedPhotoUrlMain = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 40.dp, end = 24.dp)
                            .background(Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showPriceUpdateDialogForJob != null) {
        val jobToUpdate = showPriceUpdateDialogForJob!!
        AlertDialog(
            onDismissRequest = { showPriceUpdateDialogForJob = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Update Price Quote", fontWeight = FontWeight.Bold, color = Color(0xFF111827)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter the new negotiated price (in PKR) for this service.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4B5563)
                    )
                    OutlinedTextField(
                        value = listPriceInput,
                        onValueChange = { input ->
                            if (input.length <= 6) {
                                listPriceInput = input.filter { it.isDigit() }
                            }
                        },
                        label = { Text("New Price (Rs.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            focusedContainerColor = Color(0xFFF3F4F6),
                            unfocusedContainerColor = Color(0xFFF3F4F6),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = BrandBlue,
                            cursorColor = BrandBlue
                        )
                    )
                    if (listPriceError != null) {
                        Text(
                            text = listPriceError ?: "",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceInt = listPriceInput.trim().toIntOrNull()
                        if (priceInt == null || priceInt <= 0) {
                            listPriceError = "Please enter a valid price."
                        } else {
                            viewModel.updateBookingPrice(jobToUpdate.bookingId, priceInt)
                            showPriceUpdateDialogForJob = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update Price", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriceUpdateDialogForJob = null }) {
                    Text("Cancel", color = Color(0xFF4B5563))
                }
            }
        )
    }
}

@Composable
fun JobRequestCard(
    job: com.example.homeserve.data.model.Booking,
    currentProviderId: String,
    providerLatitude: Double = 0.0,
    providerLongitude: Double = 0.0,
    onAccept: (Int) -> Unit,
    onDecline: () -> Unit,
    isAcceptEnabled: Boolean = true,
    onPhotoClick: (String) -> Unit,
    onClick: () -> Unit
) {
    var showAcceptConfirmDialog by remember { mutableStateOf(false) }
    var quotePrice by remember { mutableStateOf("") }
    var quoteError by remember { mutableStateOf<String?>(null) }

    if (showAcceptConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showAcceptConfirmDialog = false },
            containerColor = Color.White,
            title = { Text("Accept Job & Write Price Quote", fontWeight = FontWeight.Bold, color = Color(0xFF111827)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your initial price quote (in PKR) for this service based on the work. You can update this price later during chat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4B5563)
                    )
                    OutlinedTextField(
                        value = quotePrice,
                        onValueChange = { input ->
                            if (input.length <= 6) {
                                quotePrice = input.filter { it.isDigit() }
                            }
                        },
                        label = { Text("Price Quote (Rs.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            cursorColor = BrandBlue
                        )
                    )
                    if (quoteError != null) {
                        Text(
                            text = quoteError ?: "",
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceInt = quotePrice.trim().toIntOrNull()
                        if (priceInt == null || priceInt <= 0) {
                            quoteError = "Please enter a valid price quote."
                            return@Button
                        }
                        onAccept(priceInt)
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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

            if (job.jobDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Problem Description",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.jobDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4B5563)
                    )
                }
            }

            val hasPhoto = job.problemPhotoUrl.isNotBlank()
            val hasAudio = job.problemAudioUrl.isNotBlank()

            if (hasPhoto || hasAudio) {
                Spacer(modifier = Modifier.height(12.dp))
                if (hasPhoto) {
                    val photoUrls = job.problemPhotoUrl.split(",").filter { it.isNotBlank() }
                    Text(text = "Problem Photos (Tap to zoom):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF111827))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(photoUrls) { url ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF3F4F6))
                                    .clickable { onPhotoClick(url) }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Problem photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (hasAudio) {
                    Text(text = "Voice Explanation:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF111827))
                    Spacer(modifier = Modifier.height(6.dp))
                    VoiceNotePlayer(audioUrl = job.problemAudioUrl)
                }
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
    onChatClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onMarkPaymentReceived: () -> Unit = {},
    onUpdatePriceClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
                if (job.jobDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Problem Description",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = job.jobDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4B5563)
                        )
                    }
                }

                val hasPhoto = job.problemPhotoUrl.isNotBlank()
                val hasAudio = job.problemAudioUrl.isNotBlank()

                if (hasPhoto || hasAudio) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (hasPhoto) {
                        val photoUrls = job.problemPhotoUrl.split(",").filter { it.isNotBlank() }
                        Text(text = "Problem Photos (Tap to zoom):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF111827))
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(photoUrls) { url ->
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF3F4F6))
                                        .clickable { onPhotoClick(url) }
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Problem photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (hasAudio) {
                        Text(text = "Voice Explanation:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF111827))
                        Spacer(modifier = Modifier.height(6.dp))
                        VoiceNotePlayer(audioUrl = job.problemAudioUrl)
                    }
                }
            }

            if (job.status == "accepted" || job.status == "in_progress") {
                val context = LocalContext.current
                Spacer(modifier = Modifier.height(16.dp))

                // Navigate button — always shown for accepted/in-progress
                Button(
                    onClick = { launchMapsNavigation(context, job.customerLatitude, job.customerLongitude, job.address) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White)
                ) {
                    Text("🧭", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Navigate to Customer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Price Update Button inside card
                Button(
                    onClick = onUpdatePriceClick,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF2563EB))
                ) {
                    Text("💵", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Price", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (job.customerPhone.isNotBlank()) {
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
                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
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
                        Spacer(modifier = Modifier.height(10.dp))
                    }

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

            // Payment status section for completed jobs
            if (job.status == "completed") {
                Spacer(modifier = Modifier.height(12.dp))
                if (job.paymentStatus == "paid") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFECFDF5), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✅", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Payment Received",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF16A34A)
                        )
                    }
                } else {
                    Button(
                        onClick = onMarkPaymentReceived,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFECFDF5),
                            contentColor = Color(0xFF16A34A)
                        )
                    ) {
                        Text("💵", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark Payment Received", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderJobDetailsOverlay(
    job: com.example.homeserve.data.model.Booking,
    providerLatitude: Double = 0.0,
    providerLongitude: Double = 0.0,
    isAcceptEnabled: Boolean = true,
    onAccept: (Int) -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit,
    onChatClick: () -> Unit,
    onMarkPaymentReceived: () -> Unit = {},
    onUpdatePrice: (Int) -> Unit,
    onClose: () -> Unit
) {
    var zoomedPhotoUrl by remember { mutableStateOf<String?>(null) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF9FAFB)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandBlue, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .padding(top = 40.dp, bottom = 20.dp, start = 16.dp, end = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Job Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }
                }

                ProviderJobDetailsContent(
                    job = job,
                    providerLatitude = providerLatitude,
                    providerLongitude = providerLongitude,
                    isAcceptEnabled = isAcceptEnabled,
                    onAccept = { price ->
                        onAccept(price)
                        onClose()
                    },
                    onDecline = {
                        onDecline()
                        onClose()
                    },
                    onCancel = {
                        onCancel()
                        onClose()
                    },
                    onChatClick = {
                        onChatClick()
                        onClose()
                    },
                    onMarkPaymentReceived = onMarkPaymentReceived,
                    onUpdatePrice = onUpdatePrice,
                    onPhotoClick = { zoomedPhotoUrl = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (zoomedPhotoUrl != null) {
        AlertDialog(
            onDismissRequest = { zoomedPhotoUrl = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black.copy(alpha = 0.9f),
            title = null,
            text = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = zoomedPhotoUrl,
                        contentDescription = "Zoomed problem photo",
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    IconButton(
                        onClick = { zoomedPhotoUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 40.dp, end = 24.dp)
                            .background(Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun ProviderJobDetailsContent(
    job: com.example.homeserve.data.model.Booking,
    providerLatitude: Double = 0.0,
    providerLongitude: Double = 0.0,
    isAcceptEnabled: Boolean = true,
    onAccept: (Int) -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit,
    onChatClick: () -> Unit,
    onMarkPaymentReceived: () -> Unit = {},
    onUpdatePrice: (Int) -> Unit,
    onPhotoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAcceptConfirmDialog by remember { mutableStateOf(false) }
    var quotePrice by remember { mutableStateOf("") }
    var quoteError by remember { mutableStateOf<String?>(null) }

    var showUpdatePriceDialog by remember { mutableStateOf(false) }
    var newPriceInput by remember { mutableStateOf("") }
    var updatePriceError by remember { mutableStateOf<String?>(null) }

    if (showAcceptConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showAcceptConfirmDialog = false },
            containerColor = Color.White,
            title = { Text("Accept Job & Write Price Quote", fontWeight = FontWeight.Bold, color = Color(0xFF111827)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your initial price quote (in PKR) for this service based on the work. You can update this price later during chat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4B5563)
                    )
                    OutlinedTextField(
                        value = quotePrice,
                        onValueChange = { input ->
                            if (input.length <= 6) {
                                quotePrice = input.filter { it.isDigit() }
                            }
                        },
                        label = { Text("Price Quote (Rs.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            cursorColor = BrandBlue
                        )
                    )
                    if (quoteError != null) {
                        Text(
                            text = quoteError ?: "",
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceInt = quotePrice.trim().toIntOrNull()
                        if (priceInt == null || priceInt <= 0) {
                            quoteError = "Please enter a valid price quote."
                            return@Button
                        }
                        onAccept(priceInt)
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

    if (showUpdatePriceDialog) {
        AlertDialog(
            onDismissRequest = { showUpdatePriceDialog = false },
            containerColor = Color.White,
            title = { Text("Update Price Quote", fontWeight = FontWeight.Bold, color = Color(0xFF111827)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter the new negotiated price (in PKR) for this service.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4B5563)
                    )
                    OutlinedTextField(
                        value = newPriceInput,
                        onValueChange = { input ->
                            if (input.length <= 6) {
                                newPriceInput = input.filter { it.isDigit() }
                            }
                        },
                        label = { Text("New Price (Rs.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            cursorColor = BrandBlue
                        )
                    )
                    if (updatePriceError != null) {
                        Text(
                            text = updatePriceError ?: "",
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceInt = newPriceInput.trim().toIntOrNull()
                        if (priceInt == null || priceInt <= 0) {
                            updatePriceError = "Please enter a valid price."
                            return@Button
                        }
                        onUpdatePrice(priceInt)
                        showUpdatePriceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Update Price", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdatePriceDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Title Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = job.serviceName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF111827)
                        )
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Rs. ${job.totalAmount}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = BrandBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Requested by: ${if (job.customerName.isNotBlank()) job.customerName else "Customer #${job.userId.takeLast(4)}"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Status: ${job.status.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = BrandBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location / Time Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📍", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Address", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                            Text(text = job.address, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF374151))
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📅", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Schedule Time", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                            Text(text = "Today (Flexible Time / ASAP)", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF374151))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description Section
            if (job.jobDescription.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Problem Description",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = job.jobDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4B5563),
                            lineHeight = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Attachments Section (Photo & Voice note)
            val hasPhoto = job.problemPhotoUrl.isNotBlank()
            val hasAudio = job.problemAudioUrl.isNotBlank()

            if (hasPhoto || hasAudio) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Attachments",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (hasPhoto) {
                            val photoUrls = job.problemPhotoUrl.split(",").filter { it.isNotBlank() }
                            Text(text = "Problem Photos (Tap to zoom):", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(photoUrls) { url ->
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF3F4F6))
                                            .clickable { onPhotoClick(url) }
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Problem photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (hasAudio) {
                            Text(text = "Voice Explanation:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                            Spacer(modifier = Modifier.height(8.dp))
                            VoiceNotePlayer(audioUrl = job.problemAudioUrl)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Action Buttons at Bottom
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 16.dp,
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                if (job.status == "pending") {
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
                            Text(text = "Accept Job", fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (job.status == "accepted" || job.status == "in_progress") {
                    // Navigate button — primary CTA for active jobs
                    Button(
                        onClick = { launchMapsNavigation(context, job.customerLatitude, job.customerLongitude, job.address) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White)
                    ) {
                        Text("🧭", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Navigate to Customer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            newPriceInput = job.totalAmount.toString()
                            updatePriceError = null
                            showUpdatePriceDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF2563EB))
                    ) {
                        Text("💵 Update Negotiated Price", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (job.customerPhone.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Call", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onChatClick,
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF2563EB))
                            ) {
                                Text("💬", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chat", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Button(
                            onClick = onChatClick,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF2563EB))
                        ) {
                            Text("💬", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chat with Customer", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626))
                    ) {
                        Text("Cancel Job Request", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Payment section for completed jobs
    if (job.status == "completed") {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                if (job.paymentStatus == "paid") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFECFDF5), RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✅", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Payment Received",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF16A34A)
                            )
                            Text(
                                text = "Rs. ${job.totalAmount} collected",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4B5563)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onMarkPaymentReceived,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFECFDF5),
                            contentColor = Color(0xFF16A34A)
                        )
                    ) {
                        Text("💵", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark Payment Received", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceNotePlayer(
    audioUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var position by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer != null) {
                try {
                    val mp = mediaPlayer
                    if (mp != null && mp.isPlaying) {
                        position = mp.currentPosition
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(250)
            }
        }
    }

    DisposableEffect(audioUrl) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer = null
            isPlaying = false
        }
    }

    fun togglePlay() {
        if (isPlaying) {
            try {
                mediaPlayer?.pause()
                isPlaying = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                if (mediaPlayer == null) {
                    val mp = android.media.MediaPlayer().apply {
                        setDataSource(audioUrl)
                        prepareAsync()
                        setOnPreparedListener {
                            duration = it.duration
                            it.start()
                            isPlaying = true
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            position = 0
                        }
                        setOnErrorListener { _, _, _ ->
                            isPlaying = false
                            Toast.makeText(context, "Error playing audio", Toast.LENGTH_SHORT).show()
                            true
                        }
                    }
                    mediaPlayer = mp
                } else {
                    mediaPlayer?.start()
                    isPlaying = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to play audio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { togglePlay() },
            modifier = Modifier
                .background(BrandBlue, androidx.compose.foundation.shape.CircleShape)
                .size(36.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Voice Note from Customer",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = BrandBlue
            )
            Spacer(modifier = Modifier.height(4.dp))
            val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = BrandBlue,
                trackColor = BrandBlue.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            val posSec = position / 1000
            val durSec = duration / 1000
            Text(
                text = String.format(java.util.Locale.US, "%02d:%02d / %02d:%02d", posSec / 60, posSec % 60, durSec / 60, durSec % 60),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6B7280)
            )
        }
    }
}

/**
 * Launches Google Maps (or any geo-intent compatible app) for turn-by-turn navigation
 * to the customer's location.
 *
 * Priority:
 * 1. If valid lat/lng → use precise coordinates for navigation
 * 2. If lat/lng = 0,0 → fall back to address text search
 */
fun launchMapsNavigation(
    context: android.content.Context,
    latitude: Double,
    longitude: Double,
    address: String
) {
    try {
        val uri = if (latitude != 0.0 && longitude != 0.0) {
            // Precise coordinates — direct navigation mode
            Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
        } else {
            // Address fallback — text geocoding
            val encoded = Uri.encode(address)
            Uri.parse("geo:0,0?q=$encoded")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        // If Google Maps is installed, use it; otherwise, use any geo-intent app
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback: open without specifying Google Maps package
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(fallbackIntent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(
            context,
            "Could not open Maps. Please install Google Maps.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

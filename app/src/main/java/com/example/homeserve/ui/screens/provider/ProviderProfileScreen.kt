package com.example.homeserve.ui.screens.provider

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.ProviderViewModel
import android.widget.Toast
import com.example.homeserve.data.NetworkUtils
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun ProviderProfileScreen(
    viewModel: ProviderViewModel,
    onLogoutClick: () -> Unit,
    onSwitchToCustomerClick: () -> Unit,
    onAvailabilityClick: () -> Unit,
    onPayoutClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onEditProfileClick: () -> Unit
) {
    val profile by viewModel.providerProfile.collectAsState()
    val completedJobs by viewModel.completedJobs.collectAsState()
    val context = LocalContext.current
    var profilePhotoBitmap by remember(profile?.profilePhotoUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(profile?.profilePhotoUrl) {
        profile?.profilePhotoUrl?.let { url ->
            if (url.startsWith("content://")) {
                try {
                    val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(url))
                    profilePhotoBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProviderData()
    }

    val scrollState = rememberScrollState()
    var showSwitchDialog by remember { mutableStateOf(false) }
    var showEditAddressDialog by remember { mutableStateOf(false) }

    if (showSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchDialog = false },
            title = { Text("Switch Mode") },
            text = { Text("Are you sure you want to switch to Customer mode? You will need to switch back to receive job requests.") },
            confirmButton = {
                TextButton(onClick = { 
                    showSwitchDialog = false 
                    onSwitchToCustomerClick()
                }) {
                    Text("Switch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditAddressDialog) {
        var addressInput by remember { mutableStateOf(profile?.address ?: "") }
        var selectedRadius by remember { mutableStateOf(profile?.radiusKm ?: 5) }

        var gpsLat by remember { mutableStateOf(profile?.providerLatitude ?: 31.45036) }
        var gpsLon by remember { mutableStateOf(profile?.providerLongitude ?: 74.35334) }
        var useRealGps by remember { mutableStateOf(profile?.providerLatitude != 0.0 && profile?.providerLatitude != 31.45036 && profile?.providerLatitude != 31.1165) }
        var locationMethod by remember { mutableStateOf(if (useRealGps) "gps" else "manual") }
        var gpsStatusText by remember { mutableStateOf(if (useRealGps) "GPS Coords: ${String.format("%.4f", gpsLat)}, ${String.format("%.4f", gpsLon)}" else "") }

        val context = LocalContext.current
        val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineGranted || coarseGranted) {
                try {
                    gpsStatusText = "Fetching GPS location..."
                    val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                        .addOnSuccessListener { location ->
                            cancellationTokenSource.cancel()
                            if (location != null) {
                                gpsLat = location.latitude
                                gpsLon = location.longitude
                                useRealGps = true
                                
                                // Try reverse geocoding to get actual readable address
                                var detectedAddress = "Current GPS Location"
                                try {
                                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                    if (!addresses.isNullOrEmpty()) {
                                        detectedAddress = addresses[0].getAddressLine(0) ?: "Current GPS Location"
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                
                                addressInput = detectedAddress
                                gpsStatusText = "GPS Coords: ${String.format("%.4f", gpsLat)}, ${String.format("%.4f", gpsLon)}"
                            } else {
                                gpsStatusText = "Failed. Using manual address."
                            }
                        }
                        .addOnFailureListener {
                            cancellationTokenSource.cancel()
                            gpsStatusText = "Failed. Using manual address."
                        }
                } catch (e: SecurityException) {
                    gpsStatusText = "GPS Permission Error."
                }
            } else {
                gpsStatusText = "Permission denied. Using manual address."
            }
        }

        AlertDialog(
            onDismissRequest = { showEditAddressDialog = false },
            containerColor = Color.White,
            title = { Text("Edit Address & Location", fontWeight = FontWeight.Bold, color = Color(0xFF111827)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Location Method Toggle Options
                    Text(
                        text = "Location Method",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { 
                                locationMethod = "gps" 
                                addressInput = if (useRealGps) "Current GPS Location" else ""
                                if (!useRealGps) gpsStatusText = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (locationMethod == "gps") BrandBlue else Color(0xFFF3F4F6),
                                contentColor = if (locationMethod == "gps") Color.White else Color(0xFF4B5563)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Detect GPS", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }
                        Button(
                            onClick = { 
                                locationMethod = "manual" 
                                if (addressInput == "Current GPS Location") {
                                    addressInput = ""
                                }
                                gpsStatusText = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (locationMethod == "manual") BrandBlue else Color(0xFFF3F4F6),
                                contentColor = if (locationMethod == "manual") Color.White else Color(0xFF4B5563)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Manual Address", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Address Text Field
                    Text(
                        text = "Service Address",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        placeholder = { 
                            Text(
                                text = if (locationMethod == "gps") "Address will be detected via GPS" else "Enter manual address (Model Town, DHA, etc.)", 
                                color = Color(0xFF9CA3AF)
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = (locationMethod == "manual"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            disabledTextColor = Color(0xFF6B7280),
                            focusedContainerColor = if (locationMethod == "manual") Color(0xFFF3F4F6) else Color(0xFFE5E7EB),
                            unfocusedContainerColor = if (locationMethod == "manual") Color(0xFFF3F4F6) else Color(0xFFE5E7EB),
                            disabledContainerColor = Color(0xFFE5E7EB).copy(alpha = 0.6f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = BrandBlue,
                            disabledBorderColor = Color.Transparent,
                            cursorColor = BrandBlue
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Location Action Button (under the Address text field)
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        if (locationMethod == "gps") {
                            // Detect Live Location Button
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = BrandBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.3f))
                            ) {
                                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Detect Live Location", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Locate and Check Location Button
                            Button(
                                onClick = {
                                    if (addressInput.isNotBlank()) {
                                        var found = false
                                        try {
                                            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                            val addresses = geocoder.getFromLocationName(addressInput, 1)
                                            if (!addresses.isNullOrEmpty()) {
                                                gpsLat = addresses[0].latitude
                                                gpsLon = addresses[0].longitude
                                                found = true
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        
                                        if (!found) {
                                            val addrLower = addressInput.lowercase()
                                            gpsLat = when {
                                                addrLower.contains("model town") -> 31.4789
                                                addrLower.contains("gulberg") -> 31.5204
                                                addrLower.contains("dha") -> 31.4764
                                                addrLower.contains("johar town") -> 31.4697
                                                addrLower.contains("kalma chowk") || addrLower.contains("kalma chawk") -> 31.5036
                                                else -> 31.45036
                                            }
                                            gpsLon = when {
                                                addrLower.contains("model town") -> 74.3216
                                                addrLower.contains("gulberg") -> 74.3587
                                                addrLower.contains("dha") -> 74.4072
                                                addrLower.contains("johar town") -> 74.2728
                                                addrLower.contains("kalma chowk") || addrLower.contains("kalma chawk") -> 74.3321
                                                else -> 74.35334
                                            }
                                        }
                                        useRealGps = false
                                        gpsStatusText = "Checked location: Lat ${String.format(java.util.Locale.US, "%.4f", gpsLat)}, Lon ${String.format(java.util.Locale.US, "%.4f", gpsLon)}"
                                    } else {
                                        gpsStatusText = "Please enter an address first!"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFECFDF5), contentColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                            ) {
                                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Locate and Check Location", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        if (gpsStatusText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = gpsStatusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (gpsStatusText.contains("successfully") || gpsStatusText.contains("Coords:") || gpsStatusText.contains("location:")) Color(0xFF059669) else Color(0xFFDC2626),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Service Radius
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Service Radius",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF374151)
                            )
                            Text(
                                text = "$selectedRadius km",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = selectedRadius.coerceIn(2, 50).toFloat(),
                            onValueChange = { selectedRadius = it.toInt() },
                            valueRange = 2f..50f,
                            steps = 48,
                            colors = SliderDefaults.colors(
                                activeTrackColor = BrandBlue,
                                thumbColor = BrandBlue
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            listOf(15, 25, 50, 75, 100).forEach { preset ->
                                val isSelected = selectedRadius == preset
                                SuggestionChip(
                                    onClick = { selectedRadius = preset },
                                    label = { Text("$preset km", fontSize = 11.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isSelected) BrandBlue.copy(alpha = 0.1f) else Color.White,
                                        labelColor = if (isSelected) BrandBlue else Color(0xFF4B5563)
                                    ),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        enabled = true,
                                        borderColor = if (isSelected) BrandBlue else Color(0xFFD1D5DB)
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = if (locationMethod == "gps" && useRealGps) {
                            gpsLat
                        } else {
                            var resolvedLat = 31.45036
                            var found = false
                            try {
                                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                val addresses = geocoder.getFromLocationName(addressInput, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    resolvedLat = addresses[0].latitude
                                    found = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (!found) {
                                val addrLower = addressInput.lowercase()
                                resolvedLat = when {
                                    addrLower.contains("model town") -> 31.4789
                                    addrLower.contains("gulberg") -> 31.5204
                                    addrLower.contains("dha") -> 31.4764
                                    addrLower.contains("johar town") -> 31.4697
                                    addrLower.contains("kalma chowk") || addrLower.contains("kalma chawk") -> 31.5036
                                    else -> 31.45036
                                }
                            }
                            resolvedLat
                        }
                        val lon = if (locationMethod == "gps" && useRealGps) {
                            gpsLon
                        } else {
                            var resolvedLon = 74.35334
                            var found = false
                            try {
                                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                val addresses = geocoder.getFromLocationName(addressInput, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    resolvedLon = addresses[0].longitude
                                    found = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (!found) {
                                val addrLower = addressInput.lowercase()
                                resolvedLon = when {
                                    addrLower.contains("model town") -> 74.3216
                                    addrLower.contains("gulberg") -> 74.3587
                                    addrLower.contains("dha") -> 74.4072
                                    addrLower.contains("johar town") -> 74.2728
                                    addrLower.contains("kalma chowk") || addrLower.contains("kalma chawk") -> 74.3321
                                    else -> 74.35334
                                }
                            }
                            resolvedLon
                        }
                        if (NetworkUtils.isNetworkAvailable(context)) {
                            viewModel.updateProviderAddress(addressInput, selectedRadius, lat, lon)
                            showEditAddressDialog = false
                        } else {
                            Toast.makeText(context, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAddressDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                }
            }
        )
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
                .padding(top = 48.dp, bottom = 40.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Text(
                    text = "My Profile",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "Manage your provider account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Main Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val photoUrl = profile?.profilePhotoUrl ?: ""
                                if (photoUrl.isNotBlank()) {
                                    coil.compose.AsyncImage(
                                        model = photoUrl,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else if (profilePhotoBitmap != null) {
                                    Image(
                                        bitmap = profilePhotoBitmap!!.asImageBitmap(),
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(text = "👨‍🔧", fontSize = 40.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.name ?: "Service Provider", 
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF111827)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Text(text = " ${profile?.rating ?: 4.8}", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = " (${completedJobs.size} Completed Jobs)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ProfileDetailItem(icon = Icons.Default.Phone, value = profile?.phone?.takeIf { it.isNotEmpty() } ?: (if (viewModel.loggedInPhone.contains("@")) "Not Provided" else viewModel.loggedInPhone))
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileDetailItem(icon = Icons.Default.Email, value = profile?.email?.takeIf { it.isNotEmpty() } ?: (if (viewModel.loggedInPhone.contains("@")) viewModel.loggedInPhone else "Not Provided"))
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileDetailItem(icon = Icons.Default.LocationOn, value = profile?.address ?: "Lahore, PK")
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileDetailItem(icon = Icons.Default.Explore, value = "Service Radius: ${profile?.radiusKm ?: 5} km")
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = onEditProfileClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Color(0xFF374151))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Edit Profile & Address", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))


            // Settings Section
            Text(text = "Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(12.dp))
            
            MenuOption(
                icon = Icons.Default.Settings,
                title = "Availability & Schedule",
                onClick = onAvailabilityClick
            )
            Spacer(modifier = Modifier.height(8.dp))
            MenuOption(
                icon = Icons.Default.Payments,
                title = "Payout Methods",
                onClick = onPayoutClick
            )
            Spacer(modifier = Modifier.height(8.dp))
            MenuOption(
                icon = Icons.Default.Notifications,
                title = "Notification Preferences",
                onClick = onNotificationsClick
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Logout
            Surface(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Log Out", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ProfileDetailItem(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF9CA3AF))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = value, color = Color(0xFF6B7280), fontSize = 14.sp)
    }
}

@Composable
fun MenuOption(
    icon: ImageVector, 
    title: String, 
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF4B5563))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title, 
                modifier = Modifier.weight(1f), 
                fontWeight = FontWeight.Medium, 
                color = Color(0xFF111827),
                fontSize = 15.sp
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(18.dp))
        }
    }
}

package com.example.homeserve.ui.screens.provider

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.ProviderViewModel
import com.example.homeserve.data.NetworkUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditProfileScreen(
    viewModel: ProviderViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val providerProfile by viewModel.providerProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var name by remember(providerProfile) { mutableStateOf(providerProfile?.name ?: "") }
    var email by remember(providerProfile) { mutableStateOf(providerProfile?.email ?: "") }
    var bio by remember(providerProfile) { mutableStateOf(providerProfile?.bio ?: "") }
    var addressInput by remember(providerProfile) { mutableStateOf(providerProfile?.address ?: "") }
    var phone by remember(providerProfile) { mutableStateOf(providerProfile?.phone ?: viewModel.loggedInPhone) }
    var selectedRadius by remember(providerProfile) { mutableStateOf(providerProfile?.radiusKm ?: 5) }
    var passwordInput by remember(providerProfile) { mutableStateOf(providerProfile?.password ?: "") }

    // Coordinates states
    var gpsLat by remember(providerProfile) { mutableStateOf(providerProfile?.providerLatitude ?: 31.45036) }
    var gpsLon by remember(providerProfile) { mutableStateOf(providerProfile?.providerLongitude ?: 74.35334) }
    var useRealGps by remember(providerProfile) {
        mutableStateOf(
            providerProfile?.providerLatitude != null && 
            providerProfile?.providerLatitude != 0.0 && 
            providerProfile?.providerLatitude != 31.45036
        )
    }
    var locationMethod by remember(useRealGps) { mutableStateOf(if (useRealGps) "gps" else "manual") }
    var gpsStatusText by remember { mutableStateOf("") }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var selectedPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) selectedPhotoUri = uri }

    val scrollState = rememberScrollState()

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
                                val geocoder = android.location.Geocoder(context, Locale.getDefault())
                                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    detectedAddress = addresses[0].getAddressLine(0) ?: "Current GPS Location"
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            
                            addressInput = detectedAddress
                            gpsStatusText = "GPS Coords: ${String.format(Locale.US, "%.4f", gpsLat)}, ${String.format(Locale.US, "%.4f", gpsLon)}"
                        } else {
                            gpsStatusText = "Failed to detect. Please type address manually."
                        }
                    }
                    .addOnFailureListener {
                        cancellationTokenSource.cancel()
                        gpsStatusText = "Failed. Please type address manually."
                    }
            } catch (e: SecurityException) {
                gpsStatusText = "GPS Permission Error."
            }
        } else {
            gpsStatusText = "Permission denied. Please enter address manually."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Top App Bar
        Surface(
            color = BrandBlue,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 16.dp, start = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Edit Provider Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo — tappable
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
                ) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(2.dp, BrandBlue.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val photoToShow = selectedPhotoUri?.toString() ?: providerProfile?.profilePhotoUrl
                            if (!photoToShow.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoToShow,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(text = "👨‍🔧", fontSize = 48.sp)
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .size(30.dp)
                            .offset(x = (-4).dp, y = (-4).dp),
                        shape = CircleShape,
                        color = BrandBlue,
                        border = BorderStroke(2.dp, Color.White)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (selectedPhotoUri != null) "Tap to change photo" else "Tap to add photo",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Profile Fields Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        
                        // Full Name
                        Text(
                            text = "Full Name",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter your full name", color = Color(0xFF9CA3AF)) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF9CA3AF)) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
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

                        Spacer(modifier = Modifier.height(20.dp))

                        // Email Address
                        Text(
                            text = "Email Address",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("email@example.com", color = Color(0xFF9CA3AF)) },
                            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null, tint = Color(0xFF9CA3AF)) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
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

                        Spacer(modifier = Modifier.height(20.dp))

                        // Bio
                        Text(
                            text = "Professional Bio",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Brief description of your expertise and experience", color = Color(0xFF9CA3AF)) },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF9CA3AF)) },
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
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

                        Spacer(modifier = Modifier.height(20.dp))

                        // Location Setup Toggle
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
                                    addressInput = if (useRealGps) addressInput else "Current GPS Location"
                                    if (!useRealGps) gpsStatusText = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (locationMethod == "gps") BrandBlue else Color(0xFFF3F4F6),
                                    contentColor = if (locationMethod == "gps") Color.White else Color(0xFF4B5563)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("Detect GPS", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
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
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("Manual Address", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Service Address
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
                                    text = if (locationMethod == "gps") "Address detected via GPS" else "Enter service address (Model Town, DHA, etc.)", 
                                    color = Color(0xFF9CA3AF)
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = (locationMethod == "manual"),
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF9CA3AF)) },
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

                        // GPS Actions
                        if (locationMethod == "gps") {
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
                            Button(
                                onClick = {
                                    if (addressInput.isNotBlank()) {
                                        var found = false
                                        try {
                                            val geocoder = android.location.Geocoder(context, Locale.getDefault())
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
                                        gpsStatusText = "Checked: Lat ${String.format(Locale.US, "%.4f", gpsLat)}, Lon ${String.format(Locale.US, "%.4f", gpsLon)}"
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
                                color = if (gpsStatusText.contains("GPS Coords:") || gpsStatusText.contains("Checked:")) Color(0xFF059669) else Color(0xFFDC2626),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

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

                        Spacer(modifier = Modifier.height(20.dp))

                        // Phone Number (Locked/Read Only)
                        Text(
                            text = "Mobile Number (Locked)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF9CA3AF)) },
                            trailingIcon = { Icon(Icons.Default.Lock, contentDescription = "Locked Field", tint = Color(0xFF9CA3AF)) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF9CA3AF),
                                unfocusedTextColor = Color(0xFF9CA3AF),
                                focusedContainerColor = Color(0xFFE5E7EB),
                                unfocusedContainerColor = Color(0xFFE5E7EB),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                        Spacer(modifier = Modifier.height(20.dp))

                        // Change Password Row Option
                        Surface(
                            onClick = { showPasswordDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = BrandBlue)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Change Password",
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandBlue,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandBlue)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Save Changes Button
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (email.isNotBlank() && !email.contains("@")) {
                            Toast.makeText(context, "Invalid email address", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (addressInput.isBlank() || addressInput == "Current GPS Location") {
                            Toast.makeText(context, "Address cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Determine actual coordinates
                        val finalLat = if (locationMethod == "gps" && useRealGps) {
                            gpsLat
                        } else {
                            var resolvedLat = 31.45036
                            var found = false
                            try {
                                val geocoder = android.location.Geocoder(context, Locale.getDefault())
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

                        val finalLon = if (locationMethod == "gps" && useRealGps) {
                            gpsLon
                        } else {
                            var resolvedLon = 74.35334
                            var found = false
                            try {
                                val geocoder = android.location.Geocoder(context, Locale.getDefault())
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
                            viewModel.updateProviderProfile(
                                name = name.trim(),
                                email = email.trim(),
                                bio = bio.trim(),
                                address = addressInput.trim(),
                                radiusKm = selectedRadius,
                                latitude = finalLat,
                                longitude = finalLon,
                                passwordVal = passwordInput,
                                context = context,
                                newPhotoUri = selectedPhotoUri,
                                onComplete = { success ->
                                    if (success) {
                                        showSuccessDialog = true
                                    } else {
                                        Toast.makeText(context, "Failed to save profile. Try again.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        } else {
                            Toast.makeText(context, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Save Changes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Success",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            },
            text = {
                Text(
                    text = "Your profile changes have been saved successfully!",
                    color = Color(0xFF4B5563)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Change Password Dialog
    if (showPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordError by remember { mutableStateOf<String?>(null) }
        var isNewPasswordVisible by remember { mutableStateOf(false) }
        var isConfirmPasswordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Change Password",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Your account is secured via Phone OTP and Google. Setting a profile password adds an extra layer of credentials.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )

                    // New Password
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (isNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                Icon(image, contentDescription = "Toggle password visibility")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            cursorColor = BrandBlue
                        )
                    )

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(image, contentDescription = "Toggle password visibility")
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            cursorColor = BrandBlue
                        )
                    )

                    if (passwordError != null) {
                        Text(
                            text = passwordError ?: "",
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
                        val cleanNew = newPassword.trim()
                        val cleanConf = confirmPassword.trim()

                        if (cleanNew.length < 6) {
                            passwordError = "Password must be at least 6 characters."
                            return@Button
                        }
                        if (cleanNew != cleanConf) {
                            passwordError = "Passwords do not match."
                            return@Button
                        }

                        passwordInput = cleanNew
                        passwordError = null
                        showPasswordDialog = false
                        Toast.makeText(context, "Password updated. Click 'Save Changes' to apply.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Update", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }
}

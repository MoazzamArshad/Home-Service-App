package com.example.homeserve.ui.screens.provider

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.components.PrimaryButton
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.ProviderViewModel
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun ProviderProfileSetupScreen(
    viewModel: ProviderViewModel,
    onContinueClick: () -> Unit
) {
    val profileState by viewModel.providerProfile.collectAsState()
    
    var fullName by remember { mutableStateOf(profileState?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(profileState?.phone ?: if (viewModel.loggedInPhone.contains("@")) "" else viewModel.loggedInPhone) }
    var idNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf(profileState?.address ?: "") }
    var locationMethod by remember { mutableStateOf(if (profileState?.providerLatitude != 0.0 && profileState?.providerLatitude != 31.45036 && profileState != null) "gps" else "manual") } // "gps" or "manual"
    var radiusKm by remember { mutableStateOf(profileState?.radiusKm ?: 5) }
    
    var latitude by remember { mutableStateOf(profileState?.providerLatitude ?: 31.45036) }
    var longitude by remember { mutableStateOf(profileState?.providerLongitude ?: 74.35334) }

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var gpsStatus by remember { mutableStateOf("") }
    var isGpsDetected by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            try {
                gpsStatus = "Fetching location..."
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            latitude = loc.latitude
                            longitude = loc.longitude
                            isGpsDetected = true
                            gpsStatus = "GPS Location Locked!"
                        } else {
                            gpsStatus = "GPS signal weak. Using default coordinates."
                        }
                    }
            } catch (e: SecurityException) {
                gpsStatus = "Permission error."
            }
        } else {
            gpsStatus = "Permission denied."
        }
    }

    var selectedPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedDocUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var profileBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(selectedPhotoUri) {
        selectedPhotoUri?.let { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                profileBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var uploadedFileName by remember { mutableStateOf<String?>(if (profileState != null) "id_card_front.jpg" else null) }
    var isUploading by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedDocUri = uri
            uploadedFileName = uri.lastPathSegment?.substringAfterLast("/") ?: "id_card_front.jpg"
        }
    }

    var gpsStatusText by remember { mutableStateOf(if (profileState?.providerLatitude != 0.0 && profileState?.providerLatitude != 31.45036 && profileState != null) "GPS Coords: ${String.format("%.4f", profileState?.providerLatitude)}, ${String.format("%.4f", profileState?.providerLongitude)}" else "") }
    var useRealGps by remember { mutableStateOf(profileState?.providerLatitude != 0.0 && profileState?.providerLatitude != 31.45036 && profileState != null) }

    LaunchedEffect(profileState) {
        profileState?.let { profile ->
            if (fullName.isEmpty()) fullName = profile.name
            val defaultPhone = if (viewModel.loggedInPhone.contains("@")) "" else viewModel.loggedInPhone
            if (phoneNumber.isEmpty() || phoneNumber == viewModel.loggedInPhone || phoneNumber.contains("@")) {
                phoneNumber = if (profile.phone.contains("@")) defaultPhone else profile.phone.ifBlank { defaultPhone }
            }
            if (address.isEmpty()) address = profile.address
            if (profile.providerLatitude != 0.0 && profile.providerLatitude != 31.45036) {
                locationMethod = "gps"
                useRealGps = true
                gpsStatusText = "GPS Coords: ${String.format("%.4f", profile.providerLatitude)}, ${String.format("%.4f", profile.providerLongitude)}"
            } else {
                locationMethod = "manual"
            }
            radiusKm = profile.radiusKm
            latitude = profile.providerLatitude
            longitude = profile.providerLongitude
            if (uploadedFileName == null) {
                uploadedFileName = "id_card_front.jpg"
            }
        }
    }
    
    val gpsAddressPermissionLauncher = rememberLauncherForActivityResult(
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
                            latitude = location.latitude
                            longitude = location.longitude
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
                            
                            address = detectedAddress
                            gpsStatusText = "GPS Coords: ${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
                        } else {
                            gpsStatusText = "Failed to detect location."
                        }
                    }
                    .addOnFailureListener {
                        cancellationTokenSource.cancel()
                        gpsStatusText = "Failed to detect location."
                    }
            } catch (e: SecurityException) {
                gpsStatusText = "GPS Permission Error."
            }
        } else {
            gpsStatusText = "Permission denied. Using manual address entry."
        }
    }

    // Basic validation
    val isFormValid = fullName.isNotBlank() && 
                      phoneNumber.length >= 10 && 
                      idNumber.isNotBlank() && 
                      address.isNotBlank() &&
                      (locationMethod == "manual" || useRealGps) &&
                      uploadedFileName != null

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                    text = "Complete Your Profile",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Help us know you better",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Profile Photo
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
            ) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = Color(0xFFF3F4F6)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap!!.asImageBitmap(),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(text = "👤", fontSize = 48.sp)
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .size(32.dp)
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
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Upload Profile Photo", 
                style = MaterialTheme.typography.bodySmall, 
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(32.dp))

            ProfileInputField(
                label = "Full Name *", 
                value = fullName, 
                onValueChange = { fullName = it }, 
                placeholder = "Enter your full name",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            ProfileInputField(
                label = "Phone Number *", 
                value = phoneNumber, 
                onValueChange = { phoneNumber = it.filter { char -> char.isDigit() } }, 
                placeholder = "000 000 0000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
            )
            ProfileInputField(
                label = "ID Number (CNIC/SSN) *", 
                value = idNumber, 
                onValueChange = { idNumber = it }, 
                placeholder = "Enter your ID number",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            // Location Selection Options
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Text(
                    text = "Location Method *",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: Detect Location
                    Button(
                        onClick = { 
                            locationMethod = "gps" 
                            address = if (useRealGps) "Current GPS Location" else ""
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
                    // Option 2: Manual Address
                    Button(
                        onClick = { 
                            locationMethod = "manual" 
                            if (address == "Current GPS Location") {
                                address = ""
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
            }

            // Address Input Field
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Text(
                    text = "Address *",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = { 
                        Text(
                            text = if (locationMethod == "gps") "Address will be detected via GPS" else "Enter manual address (Model Town, DHA, etc.)", 
                            color = Color(0xFF9CA3AF)
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = (locationMethod == "manual"), // Disable/blur if GPS is selected
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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
            }

            // Location Action Button (under the Address text field)
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                if (locationMethod == "gps") {
                    // Detect Live Location Button
                    Button(
                        onClick = {
                            gpsAddressPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.3f))
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detect Live Location", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Locate and Check Location Button
                    Button(
                        onClick = {
                            if (address.isNotBlank()) {
                                var found = false
                                try {
                                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                    val addresses = geocoder.getFromLocationName(address, 1)
                                    if (!addresses.isNullOrEmpty()) {
                                        latitude = addresses[0].latitude
                                        longitude = addresses[0].longitude
                                        found = true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                
                                if (!found) {
                                    val addrLower = address.lowercase()
                                    latitude = when {
                                        addrLower.contains("model town") -> 31.4789
                                        addrLower.contains("gulberg") -> 31.5204
                                        addrLower.contains("dha") -> 31.4764
                                        addrLower.contains("johar town") -> 31.4697
                                        addrLower.contains("kalma chowk") || addrLower.contains("kalma chawk") -> 31.5036
                                        else -> 31.45036
                                    }
                                    longitude = when {
                                        addrLower.contains("model town") -> 74.3216
                                        addrLower.contains("gulberg") -> 74.3587
                                        addrLower.contains("dha") -> 74.4072
                                        addrLower.contains("johar town") -> 74.2728
                                        addrLower.contains("kalma chowk") || addrLower.contains("kalma chawk") -> 74.3321
                                        else -> 74.35334
                                    }
                                }
                                useRealGps = false
                                gpsStatusText = "Checked location coordinates successfully!"
                            } else {
                                gpsStatusText = "Please enter an address first!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFECFDF5), contentColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
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
                        color = if (locationMethod == "gps" && useRealGps) Color(0xFF059669) else if (locationMethod == "manual" && gpsStatusText.contains("successfully")) Color(0xFF059669) else Color(0xFFDC2626),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // Service Radius Slider
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Text(
                    text = "Service Radius *",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = radiusKm.toFloat(),
                        onValueChange = { radiusKm = it.toInt() },
                        valueRange = 2f..25f,
                        steps = 22,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = BrandBlue,
                            thumbColor = BrandBlue
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "$radiusKm km",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111827)
                    )
                }
            }



            // Upload Documents
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text(
                    text = "Upload ID Card Front Pic *",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            if (uploadedFileName != null) Color(0xFFECFDF5) else Color(0xFFF9FAFB), 
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp, 
                            color = if (uploadedFileName != null) Color(0xFF10B981) else Color(0xFFE5E7EB), 
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { 
                            documentPickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uploadedFileName != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "✅",
                                fontSize = 32.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uploadedFileName!!, 
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), 
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "Verification Pending (Tap to replace)", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = Color(0xFF059669)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FileUpload, 
                                contentDescription = null, 
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to upload ID Card Front Pic", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "(Max size 5MB, PNG/JPG)", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = Color(0xFF9CA3AF).copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                PrimaryButton(
                    text = "Continue",
                    onClick = {
                        val finalLat = if (locationMethod == "gps" && useRealGps) {
                            latitude
                        } else {
                            var resolvedLat = 31.45036
                            var found = false
                            try {
                                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                val addresses = geocoder.getFromLocationName(address, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    resolvedLat = addresses[0].latitude
                                    found = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (!found) {
                                val addrLower = address.lowercase()
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
                            longitude
                        } else {
                            var resolvedLon = 74.35334
                            var found = false
                            try {
                                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                val addresses = geocoder.getFromLocationName(address, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    resolvedLon = addresses[0].longitude
                                    found = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (!found) {
                                val addrLower = address.lowercase()
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
                        viewModel.saveTempProfileInfo(
                            fullName,
                            phoneNumber,
                            idNumber,
                            address,
                            radiusKm,
                            finalLat,
                            finalLon,
                            selectedPhotoUri?.toString() ?: "",
                            selectedDocUri?.toString() ?: ""
                        )
                        onContinueClick()
                    },
                    enabled = isFormValid,
                    modifier = Modifier.height(54.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF374151),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Color(0xFF9CA3AF)) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = keyboardOptions,
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
    }
}

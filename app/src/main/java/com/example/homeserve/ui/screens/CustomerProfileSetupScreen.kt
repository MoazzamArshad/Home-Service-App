package com.example.homeserve.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.homeserve.ui.components.PrimaryButton
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.CustomerViewModel
import com.example.homeserve.data.NetworkUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun CustomerProfileSetupScreen(
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerViewModel = viewModel()
) {
    val isGoogleSignIn = remember(viewModel.loggedInPhone) { viewModel.loggedInPhone.contains("@") }
    
    // States preserved across page steps
    var currentStep by remember { mutableStateOf(1) }

    androidx.activity.compose.BackHandler {
        if (currentStep == 2) {
            currentStep = 1
        } else {
            onBackClick()
        }
    }
    var name by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf(if (isGoogleSignIn) "" else viewModel.loggedInPhone) }
    var address by remember { mutableStateOf("") }
    
    var locationMethod by remember { mutableStateOf("manual") } // "manual" or "gps"
    var gpsLat by remember { mutableStateOf(31.5204) }
    var gpsLon by remember { mutableStateOf(74.3587) }
    var useRealGps by remember { mutableStateOf(false) }
    var gpsStatusText by remember { mutableStateOf("") }
    
    var selectedPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) selectedPhotoUri = uri }
    
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
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
                            gpsLat = location.latitude
                            gpsLon = location.longitude
                            useRealGps = true
                            
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
                            gpsStatusText = "GPS Coords: ${String.format("%.4f", gpsLat)}, ${String.format("%.4f", gpsLon)}"
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
    
    val isStep1Valid = name.isNotBlank() && phoneInput.length >= 10
    val isStep2Valid = address.isNotBlank() && (locationMethod == "manual" || useRealGps)

    if (isLoading) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandBlue)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Stepper Header
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
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            if (currentStep == 2) {
                                currentStep = 1
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (currentStep == 1) "Create Your Profile" else "Verify Your Location",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Step $currentStep of 2",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { if (currentStep == 1) 0.5f else 1.0f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
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
            if (currentStep == 1) {
                Spacer(modifier = Modifier.height(32.dp))

                // Profile Photo Container - Click to Upload
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
                ) {
                    Surface(
                        modifier = Modifier.size(110.dp),
                        shape = CircleShape,
                        color = Color(0xFFF3F4F6),
                        border = BorderStroke(2.dp, BrandBlue.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (selectedPhotoUri != null) {
                                AsyncImage(
                                    model = selectedPhotoUri,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(text = "👤", fontSize = 52.sp)
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
                    text = "Add Profile Photo", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Name Field (Required)
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Text(
                        text = "Full Name *",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Enter your full name", color = Color(0xFF9CA3AF)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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

                // Phone Field (Required - Unchangeable if registered via OTP)
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Text(
                        text = if (isGoogleSignIn) "Phone Number *" else "Phone Number (Verified)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { if (isGoogleSignIn) phoneInput = it.filter { char -> char.isDigit() } },
                        enabled = isGoogleSignIn,
                        placeholder = { Text("e.g. 300 1234567", color = Color(0xFF9CA3AF)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            disabledTextColor = Color(0xFF6B7280),
                            focusedContainerColor = Color(0xFFF3F4F6),
                            unfocusedContainerColor = Color(0xFFF3F4F6),
                            disabledContainerColor = Color(0xFFE5E7EB),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = BrandBlue,
                            cursorColor = BrandBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "A profile photo and verified name help service providers identify you easily.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                Spacer(modifier = Modifier.height(24.dp))

                // Location Setup Toggle
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
                        Button(
                            onClick = { 
                                locationMethod = "gps" 
                                address = if (useRealGps) address else ""
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

                // Address Field (Required)
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Text(
                        text = "Home Address *",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { 
                            Text(
                                text = if (locationMethod == "gps") "Address detected via GPS" else "Enter manual address (Model Town, DHA, etc.)", 
                                color = Color(0xFF9CA3AF)
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = (locationMethod == "manual"),
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

                // Location Action Buttons (Detect GPS or Manual coordinates check)
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    if (locationMethod == "gps") {
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
                        Button(
                            onClick = {
                                if (address.isNotBlank()) {
                                    var found = false
                                    try {
                                        val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                        val addresses = geocoder.getFromLocationName(address, 1)
                                        if (!addresses.isNullOrEmpty()) {
                                            gpsLat = addresses[0].latitude
                                            gpsLon = addresses[0].longitude
                                            found = true
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    
                                    if (!found) {
                                        val addrLower = address.lowercase()
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

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "A locked address or verified location lets provider partners dispatch to you immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Bottom Actions Bar
        if (currentStep == 1) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Box(modifier = Modifier.padding(24.dp)) {
                    PrimaryButton(
                        text = "Next: Set Address",
                        onClick = {
                            if (isStep1Valid) {
                                currentStep = 2
                            }
                        },
                        enabled = isStep1Valid && !isLoading,
                        modifier = Modifier.height(54.dp)
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { currentStep = 1 },
                        enabled = !isLoading,
                        modifier = Modifier.height(54.dp)
                    ) {
                        Text("Back", color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    PrimaryButton(
                        text = "Let's Get Started",
                        onClick = {
                            if (isStep1Valid && isStep2Valid) {
                                if (!NetworkUtils.isNetworkAvailable(context)) {
                                    Toast.makeText(context, "No network connection. Please check your internet and try again.", Toast.LENGTH_LONG).show()
                                } else {
                                    val finalLat = gpsLat
                                    val finalLon = gpsLon
                                    
                                    viewModel.updateCustomerProfile(
                                        name = name.trim(),
                                        address = address.trim(),
                                        phone = phoneInput.trim(),
                                        context = context,
                                        newPhotoUri = selectedPhotoUri,
                                        onResult = { success ->
                                            if (success) {
                                                // Seed the default SavedAddress
                                                val savedAddr = com.example.homeserve.data.model.SavedAddress(
                                                    id = "addr_home_" + System.currentTimeMillis(),
                                                    label = "Home",
                                                    address = address.trim(),
                                                    city = "Lahore",
                                                    state = "Punjab",
                                                    zip = "54000",
                                                    isDefault = true,
                                                    latitude = finalLat,
                                                    longitude = finalLon
                                                )
                                                viewModel.addAddress(savedAddr)
                                                
                                                onContinueClick()
                                            } else {
                                                Toast.makeText(context, "Failed to update profile. Please try again.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        enabled = isStep2Valid && !isLoading,
                        modifier = Modifier.weight(1f).height(54.dp)
                    )
                }
            }
        }
    }
}

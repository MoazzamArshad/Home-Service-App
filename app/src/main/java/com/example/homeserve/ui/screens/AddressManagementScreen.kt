package com.example.homeserve.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.components.PrimaryButton
import com.example.homeserve.ui.data.AddressItem
import com.example.homeserve.ui.data.CustomerMockData
import com.example.homeserve.ui.theme.BrandBlue
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun AddressManagementScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val addresses = remember { mutableStateListOf<AddressItem>().apply { addAll(CustomerMockData.mockAddresses) } }
    var label by remember { mutableStateOf("") }
    var streetAddress by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    var latitude by remember { mutableStateOf(31.5204) }
    var longitude by remember { mutableStateOf(74.3587) }
    var isGpsDetected by remember { mutableStateOf(false) }
    var gpsStatusText by remember { mutableStateOf("") }
    var locationMethod by remember { mutableStateOf("gps") } // "gps" or "manual"

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
                            latitude = location.latitude
                            longitude = location.longitude
                            isGpsDetected = true
                            
                            // Try reverse geocoding to get actual readable address
                            var detectedAddress = "Current GPS Location"
                            try {
                                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                val addressesResult = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                if (!addressesResult.isNullOrEmpty()) {
                                    detectedAddress = addressesResult[0].getAddressLine(0) ?: "Current GPS Location"
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            
                            streetAddress = detectedAddress
                            city = "Lahore"
                            gpsStatusText = "GPS Coords: ${String.format(java.util.Locale.US, "%.4f", latitude)}, ${String.format(java.util.Locale.US, "%.4f", longitude)}"
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
                        text = "Saved Addresses",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Manage your service locations",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Address List
            LazyColumn(
                modifier = Modifier.weight(1.5f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(addresses, key = { it.id }) { item ->
                    AddressCard(
                        address = item,
                        onDelete = { 
                            addresses.remove(item)
                            CustomerMockData.mockAddresses.removeIf { it.id == item.id }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(modifier = Modifier.height(24.dp))

            // Add New Address Section
            Text(
                text = "Add New Address",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111827)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Location Method *",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { 
                            locationMethod = "gps" 
                            streetAddress = if (isGpsDetected) streetAddress else ""
                            if (!isGpsDetected) {
                                streetAddress = ""
                                city = ""
                                gpsStatusText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (locationMethod == "gps") BrandBlue else Color(0xFFF3F4F6),
                            contentColor = if (locationMethod == "gps") Color.White else Color(0xFF4B5563)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Detect GPS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { 
                            locationMethod = "manual" 
                            if (streetAddress.startsWith("Current GPS Location") || streetAddress.startsWith("📍")) {
                                streetAddress = ""
                                city = ""
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
                        Text("Manual Address", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                AddressInputField(
                    label = "Label (e.g. Home, Work)",
                    value = label,
                    onValueChange = { label = it },
                    placeholder = "Enter label"
                )
                
                AddressInputField(
                    label = "Address *",
                    value = streetAddress,
                    onValueChange = { streetAddress = it },
                    placeholder = if (locationMethod == "gps") "Address will be detected via GPS" else "Enter manual address (Model Town, DHA, etc.)",
                    enabled = (locationMethod == "manual")
                )
                
                Spacer(modifier = Modifier.height(8.dp))

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
                        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.3f))
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detect Live Location", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (streetAddress.isNotBlank()) {
                                var found = false
                                try {
                                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                    val addressesResult = geocoder.getFromLocationName(streetAddress, 1)
                                    if (!addressesResult.isNullOrEmpty()) {
                                        latitude = addressesResult[0].latitude
                                        longitude = addressesResult[0].longitude
                                        found = true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                
                                if (!found) {
                                    val addrLower = streetAddress.lowercase()
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
                                isGpsDetected = false
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
                        color = if (locationMethod == "gps" && isGpsDetected) Color(0xFF059669) else if (locationMethod == "manual" && gpsStatusText.contains("successfully")) Color(0xFF059669) else Color(0xFFDC2626),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Save Address",
                    onClick = {
                        if (label.isNotBlank() && streetAddress.isNotBlank() && city.isNotBlank()) {
                            val finalLat = if (locationMethod == "gps" && isGpsDetected) {
                                latitude
                            } else {
                                var resolvedLat = 31.5204
                                var found = false
                                try {
                                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                    val result = geocoder.getFromLocationName(streetAddress, 1)
                                    if (!result.isNullOrEmpty()) {
                                        resolvedLat = result[0].latitude
                                        found = true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                if (!found) {
                                    val addrLower = streetAddress.lowercase()
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

                            val finalLon = if (locationMethod == "gps" && isGpsDetected) {
                                longitude
                            } else {
                                var resolvedLon = 74.3587
                                var found = false
                                try {
                                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                    val result = geocoder.getFromLocationName(streetAddress, 1)
                                    if (!result.isNullOrEmpty()) {
                                        resolvedLon = result[0].longitude
                                        found = true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                if (!found) {
                                    val addrLower = streetAddress.lowercase()
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

                            val newItem = AddressItem(
                                id = "addr-${System.currentTimeMillis()}",
                                label = label,
                                address = streetAddress,
                                city = "",
                                state = "Punjab",
                                zip = "54000",
                                isDefault = false,
                                latitude = finalLat,
                                longitude = finalLon
                            )
                            addresses.add(newItem)
                            CustomerMockData.mockAddresses.add(newItem)
                            label = ""
                            streetAddress = ""
                            city = ""
                            latitude = 31.5204
                            longitude = 74.3587
                            isGpsDetected = false
                            gpsStatusText = ""
                            locationMethod = "gps"
                        }
                    },
                    enabled = label.isNotBlank() && streetAddress.isNotBlank() && (locationMethod == "manual" || isGpsDetected)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AddressCard(
    address: AddressItem,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF3F4F6)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = if (address.label.contains("Home", ignoreCase = true)) Icons.Default.Home 
                              else if (address.label.contains("Work", ignoreCase = true)) Icons.Default.Work
                              else Icons.Default.LocationOn
                    Icon(icon, contentDescription = null, tint = Color(0xFF4B5563))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = address.label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111827)
                    )
                    if (address.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "DEFAULT",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                        }
                    }
                }
                Text(
                    text = if (address.city.isNotBlank()) "${address.address}, ${address.city}" else address.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
private fun AddressInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF374151),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Color(0xFF9CA3AF)) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF111827),
                unfocusedTextColor = Color(0xFF111827),
                disabledTextColor = Color(0xFF6B7280),
                focusedContainerColor = if (enabled) Color.White else Color(0xFFE5E7EB).copy(alpha = 0.6f),
                unfocusedContainerColor = if (enabled) Color.White else Color(0xFFE5E7EB).copy(alpha = 0.6f),
                disabledContainerColor = Color(0xFFE5E7EB).copy(alpha = 0.6f),
                focusedBorderColor = BrandBlue,
                unfocusedBorderColor = Color(0xFFE5E7EB),
                disabledBorderColor = Color(0xFFE5E7EB),
                cursorColor = BrandBlue
            )
        )
    }
}

package com.example.homeserve.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.example.homeserve.ui.data.CustomerMockData
import com.example.homeserve.ui.theme.BrandBlue
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun BookingAddressScreen(
    onBackClick: () -> Unit,
    onAddAddressClick: () -> Unit,
    onContinueClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val addresses = remember { mutableStateListOf<com.example.homeserve.ui.data.AddressItem>().apply { addAll(CustomerMockData.mockAddresses) } }
    var selectedAddressId by remember { mutableStateOf(addresses.firstOrNull()?.id ?: "") }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var newLabel by remember { mutableStateOf("") }
    var newStreetAddress by remember { mutableStateOf("") }
    var newCity by remember { mutableStateOf("") }

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var locationMethod by remember { mutableStateOf("gps") } // "gps" or "manual"
    var useRealGps by remember { mutableStateOf(false) }
    var gpsStatusText by remember { mutableStateOf("") }
    var gpsLat by remember { mutableStateOf(31.5204) }
    var gpsLon by remember { mutableStateOf(74.3587) }

    val resetLocationStates = {
        locationMethod = "gps"
        useRealGps = false
        gpsStatusText = ""
        gpsLat = 31.5204
        gpsLon = 74.3587
        newLabel = ""
        newStreetAddress = ""
        newCity = ""
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            try {
                gpsStatusText = "Locating..."
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
                                val result = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                if (!result.isNullOrEmpty()) {
                                    detectedAddress = result[0].getAddressLine(0) ?: "Current GPS Location"
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            newStreetAddress = detectedAddress
                            newCity = "Lahore"
                            gpsStatusText = "GPS Coordinates Locked!"
                        } else {
                            gpsStatusText = "GPS weak. Try manual address."
                        }
                    }
                    .addOnFailureListener {
                        cancellationTokenSource.cancel()
                        gpsStatusText = "GPS failed. Try manual address."
                    }
            } catch (e: SecurityException) {
                gpsStatusText = "Permission error."
            }
        } else {
            gpsStatusText = "Permission denied."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Header
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
                        text = "Select Address",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Step 2 of 3",
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

            // Progress Bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepBar(true, Modifier.weight(1f))
                StepBar(true, Modifier.weight(1f))
                StepBar(false, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Section header with Add New button — always visible
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Addresses",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111827)
                )
                Surface(
                    onClick = { showAddAddressDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add New",
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (addresses.isEmpty()) {
                // Empty state — guides user to add an address
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No saved addresses yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF374151)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap \"Add New\" above to add your service location.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(addresses) { address ->
                        val isSelected = selectedAddressId == address.id
                        AddressItem(
                            address = address,
                            isSelected = isSelected,
                            onClick = { selectedAddressId = address.id }
                        )
                    }
                }
            }
        }

        // Bottom Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 16.dp,
            color = Color.White
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                PrimaryButton(
                    text = "Continue to Confirmation",
                    onClick = { onContinueClick(selectedAddressId) },
                    enabled = selectedAddressId.isNotBlank()
                )
            }
        }
    }

    if (showAddAddressDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddAddressDialog = false 
                resetLocationStates()
            },
            containerColor = Color.White,
            title = { Text("Add New Address", fontWeight = FontWeight.Bold, color = Color(0xFF111827)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        label = { Text("Label (e.g. Home, Work)") },
                        placeholder = { Text("Home", color = Color(0xFF9CA3AF)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            cursorColor = BrandBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

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
                                newStreetAddress = if (useRealGps) newStreetAddress else ""
                                if (!useRealGps) {
                                    newStreetAddress = ""
                                    newCity = ""
                                    gpsStatusText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (locationMethod == "gps") BrandBlue else Color(0xFFF3F4F6),
                                contentColor = if (locationMethod == "gps") Color.White else Color(0xFF4B5563)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Detect GPS", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }
                        Button(
                            onClick = { 
                                locationMethod = "manual" 
                                if (newStreetAddress.startsWith("Current GPS Location") || newStreetAddress.startsWith("📍")) {
                                    newStreetAddress = ""
                                    newCity = ""
                                }
                                gpsStatusText = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (locationMethod == "manual") BrandBlue else Color(0xFFF3F4F6),
                                contentColor = if (locationMethod == "manual") Color.White else Color(0xFF4B5563)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Manual Address", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newStreetAddress,
                        onValueChange = { newStreetAddress = it },
                        label = { Text("Address *") },
                        placeholder = { 
                            Text(
                                text = if (locationMethod == "gps") "Address will be detected via GPS" else "Enter manual address (Model Town, DHA, etc.)", 
                                color = Color(0xFF9CA3AF)
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = (locationMethod == "manual"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            disabledTextColor = Color(0xFF6B7280),
                            focusedContainerColor = if (locationMethod == "manual") Color.White else Color(0xFFE5E7EB).copy(alpha = 0.6f),
                            unfocusedContainerColor = if (locationMethod == "manual") Color.White else Color(0xFFE5E7EB).copy(alpha = 0.6f),
                            disabledContainerColor = Color(0xFFE5E7EB).copy(alpha = 0.6f),
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            disabledBorderColor = Color(0xFFE5E7EB),
                            cursorColor = BrandBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

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
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.3f))
                        ) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detect Live Location", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (newStreetAddress.isNotBlank()) {
                                    var found = false
                                    try {
                                        val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                        val addressesResult = geocoder.getFromLocationName(newStreetAddress, 1)
                                        if (!addressesResult.isNullOrEmpty()) {
                                            gpsLat = addressesResult[0].latitude
                                            gpsLon = addressesResult[0].longitude
                                            found = true
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    
                                    if (!found) {
                                        val addrLower = newStreetAddress.lowercase()
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
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                        ) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Locate and Check Location", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newLabel.isNotBlank() && newStreetAddress.isNotBlank() && newCity.isNotBlank()) {
                            val finalLat = if (locationMethod == "gps" && useRealGps) {
                                gpsLat
                            } else {
                                var resolvedLat = 31.5204
                                var found = false
                                try {
                                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                    val result = geocoder.getFromLocationName(newStreetAddress, 1)
                                    if (!result.isNullOrEmpty()) {
                                        resolvedLat = result[0].latitude
                                        found = true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                if (!found) {
                                    val addrLower = newStreetAddress.lowercase()
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
                                var resolvedLon = 74.3587
                                var found = false
                                try {
                                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                    val result = geocoder.getFromLocationName(newStreetAddress, 1)
                                    if (!result.isNullOrEmpty()) {
                                        resolvedLon = result[0].longitude
                                        found = true
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                if (!found) {
                                    val addrLower = newStreetAddress.lowercase()
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

                            val newAddr = com.example.homeserve.ui.data.AddressItem(
                                id = "addr-${System.currentTimeMillis()}",
                                label = newLabel,
                                address = newStreetAddress,
                                city = "",
                                state = "Punjab",
                                zip = "54000",
                                isDefault = false,
                                latitude = finalLat,
                                longitude = finalLon
                            )
                            addresses.add(newAddr)
                            CustomerMockData.mockAddresses.add(newAddr)
                            selectedAddressId = newAddr.id
                            showAddAddressDialog = false
                            resetLocationStates()
                        }
                    },
                    enabled = newLabel.isNotBlank() && newStreetAddress.isNotBlank() && (locationMethod == "manual" || useRealGps),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFF3F4F6),
                        disabledContentColor = Color(0xFF9CA3AF)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Address", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAddAddressDialog = false 
                        resetLocationStates()
                    }
                ) {
                    Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun AddressItem(
    address: com.example.homeserve.ui.data.AddressItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) BrandBlue else Color(0xFFE5E7EB)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = BrandBlue)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = if (address.label.contains("Home", ignoreCase = true)) Icons.Default.Home 
                              else if (address.label.contains("Work", ignoreCase = true)) Icons.Default.Work
                              else Icons.Default.LocationOn
                    Icon(icon, contentDescription = null, tint = if (isSelected) BrandBlue else Color(0xFF6B7280), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = address.label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) BrandBlue else Color(0xFF111827)
                    )
                    if (address.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "DEFAULT",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (address.city.isNotBlank()) "${address.address}, ${address.city}" else address.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4B5563)
                )
            }
        }
    }
}

@Composable
private fun StepBar(active: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(6.dp)
            .background(
                if (active) BrandBlue else Color(0xFFE5E7EB),
                RoundedCornerShape(3.dp)
            )
    )
}

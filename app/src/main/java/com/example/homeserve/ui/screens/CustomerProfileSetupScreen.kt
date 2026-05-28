package com.example.homeserve.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homeserve.ui.components.PrimaryButton
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.CustomerViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.homeserve.data.NetworkUtils

@Composable
fun CustomerProfileSetupScreen(
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerViewModel = viewModel()
) {
    val isGoogleSignIn = remember(viewModel.loggedInPhone) { viewModel.loggedInPhone.contains("@") }
    var name by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf(if (isGoogleSignIn) "" else viewModel.loggedInPhone) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    
    val scrollState = rememberScrollState()
    val isFormValid = name.isNotBlank() && phoneInput.isNotBlank() && phoneInput.length >= 10

    Column(
        modifier = modifier
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
                    text = "Welcome to HomeServe!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please set up your profile to continue",
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
            Spacer(modifier = Modifier.height(40.dp))

            // Profile Avatar Placeholder
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color(0xFFEFF6FF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "👋", fontSize = 48.sp)
                }
            }

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

            // Phone Field (Required)
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Text(
                    text = if (isGoogleSignIn) "Phone Number *" else "Phone Number (Verified)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { if (isGoogleSignIn) phoneInput = it },
                    enabled = isGoogleSignIn,
                    placeholder = { Text("e.g. +92 300 1234567", color = Color(0xFF9CA3AF)) },
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
                text = "Providing your full name and phone number helps service providers coordinate with you for bookings.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280),
                lineHeight = 16.sp
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                PrimaryButton(
                    text = "Let's Get Started",
                    onClick = {
                        if (name.isNotBlank() && phoneInput.isNotBlank()) {
                            if (!NetworkUtils.isNetworkAvailable(context)) {
                                Toast.makeText(context, "No network connection. Please check your internet and try again.", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.updateCustomerProfile(
                                    name = name.trim(),
                                    address = "",
                                    phone = phoneInput.trim()
                                )
                                onContinueClick()
                            }
                        }
                    },
                    enabled = isFormValid && !isLoading,
                    modifier = Modifier.height(54.dp)
                )
            }
        }
    }
}

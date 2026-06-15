package com.example.homeserve.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.components.PrimaryButton
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.CustomerViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

enum class AuthMode {
    LOGIN,
    REGISTER
}

@Composable
fun LoginScreen(
    viewModel: CustomerViewModel,
    onNavigateToOtp: (String) -> Unit,
    onEmailLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf<AuthMode?>(null) }
    var phoneNumber by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()

    val isFormValid = phoneNumber.length >= 10

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
    ) {
        // Blue Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BrandBlue,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 24.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                if (currentMode != null) {
                    IconButton(
                        onClick = { currentMode = null },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = when (currentMode) {
                        AuthMode.LOGIN -> "Customer Login"
                        AuthMode.REGISTER -> "Register Account"
                        null -> "Welcome to HomeServe"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (currentMode) {
                        AuthMode.LOGIN -> "Enter your phone number to log in via OTP"
                        AuthMode.REGISTER -> "Enter your phone number to create a new customer account"
                        null -> "Your trusted home services platform"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Logo
            Surface(
                modifier = Modifier.size(90.dp),
                shape = CircleShape,
                color = Color(0xFFEFF6FF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🏠", fontSize = 44.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (currentMode == null) {
                // Initial selection screen: Login and Register buttons
                Text(
                    text = "Book Trusted Services Easily",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select an option below to access your account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                PrimaryButton(
                    text = "Log In to Your Account",
                    onClick = { currentMode = AuthMode.LOGIN },
                    modifier = Modifier.height(52.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { currentMode = AuthMode.REGISTER },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, BrandBlue),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = BrandBlue
                    )
                ) {
                    Text("Create a New Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))
            } else {
                // Phone Field (Both Modes)
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text(
                        text = "Phone Number",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { input ->
                            if (input.length <= 16) {
                                phoneNumber = input.filter { char -> char.isDigit() || char == '+' }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. +923001234567", color = Color(0xFF9CA3AF)) },
                        leadingIcon = { Icon(Icons.Outlined.Phone, null, tint = BrandBlue) },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = if (isLoading) "Sending OTP..." else if (currentMode == AuthMode.REGISTER) "Register & Send OTP" else "Send Verification Code",
                    onClick = {
                        val activity = context as? android.app.Activity
                        if (activity != null) {
                            viewModel.sendOtp(
                                phone = phoneNumber.trim(),
                                activity = activity,
                                onCodeSent = {
                                    onNavigateToOtp(phoneNumber.trim())
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Activity context is missing", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = isFormValid && !isLoading,
                    modifier = Modifier.height(50.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Toggle Login / Register
                Text(
                    text = if (currentMode == AuthMode.REGISTER) "Already have an account? Sign In" else "New to HomeServe? Register Now",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue
                    ),
                    modifier = Modifier
                        .clickable {
                            currentMode = if (currentMode == AuthMode.REGISTER) AuthMode.LOGIN else AuthMode.REGISTER
                        }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "By continuing, you agree to our Terms of Service and Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

package com.example.homeserve.ui.screens.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.components.PrimaryButton
import com.example.homeserve.ui.theme.BrandBlue

@Composable
fun ProviderOtpScreen(
    phoneNumber: String,
    onVerifyClick: () -> Unit
) {
    var generatedOtp by remember { mutableStateOf((100000..999999).random().toString()) }
    var otpCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus the field when the screen opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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
                    text = "Verify OTP",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter the 6-digit code sent to your phone",
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

            // Beautiful Banner for Mock SMS Gateway
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💬", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Mock SMS Gateway",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E40AF)
                        )
                        Text(
                            text = "Your verification code is: $generatedOtp",
                            fontSize = 13.sp,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lock Icon in Circle
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = Color(0xFFEFF6FF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🔐", fontSize = 40.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Code sent to +92 $phoneNumber",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // OTP Input Fields using BasicTextField for interaction
            Box(contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = otpCode,
                    onValueChange = { 
                        if (it.length <= 6) {
                            otpCode = it
                            if (errorMessage != null) errorMessage = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    decorationBox = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(6) { index ->
                                val char = otpCode.getOrNull(index)?.toString() ?: ""
                                val isFocused = otpCode.length == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .background(
                                            color = Color(0xFFF3F4F6), 
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .then(
                                            if (isFocused) Modifier.background(
                                                Color(0xFF2563EB).copy(alpha = 0.05f),
                                                RoundedCornerShape(12.dp)
                                            ) else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = char,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111827)
                                        )
                                    )
                                    // Caret/cursor placeholder when focused on this box
                                    if (isFocused) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(24.dp)
                                                .background(Color(0xFF2563EB))
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }

            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            PrimaryButton(
                text = "Verify & Continue",
                onClick = {
                    if (otpCode == generatedOtp) {
                        errorMessage = null
                        onVerifyClick()
                    } else {
                        errorMessage = "Invalid verification code. Please try again!"
                    }
                },
                enabled = otpCode.length == 6,
                modifier = Modifier.height(54.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Didn't receive the code? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
                TextButton(
                    onClick = {
                        generatedOtp = (100000..999999).random().toString()
                        otpCode = ""
                        errorMessage = null
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Resend OTP",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2563EB)
                    )
                }
            }
        }
    }
}

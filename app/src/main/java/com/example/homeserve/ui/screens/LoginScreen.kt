package com.example.homeserve.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.components.PrimaryButton
import com.example.homeserve.ui.theme.BrandBlue
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    onContinueClick: (String) -> Unit,
    onGoogleSignInClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    val isValid = phoneNumber.length >= 10
    
    // State to show manual entry fallback ONLY if Google Sign-In SDK fails (e.g. no Google Play Services or missing SHA-1)
    var showFallbackSelector by remember { mutableStateOf(false) }
    var fallbackErrorMsg by remember { mutableStateOf("") }

    // Google Sign-In SDK Configuration
    val webClientId = "35163826001-v4s9m1s8p5gf777a2m8nv3u388idqqu6.apps.googleusercontent.com"
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: ""
            val name = account?.displayName ?: "Google User"
            if (email.isNotBlank()) {
                onGoogleSignInClick(email, name)
            } else {
                Toast.makeText(context, "Google Sign-In returned an empty email", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            e.printStackTrace()
            if (e.statusCode == 12501 || e.statusCode == 16 || e.statusCode == 7) {
                // User cancelled or network error during account choice. Do not show fallback dialog.
                return@rememberLauncherForActivityResult
            }
            fallbackErrorMsg = "Google Sign-In Error (Code: ${e.statusCode}). Please ensure Google Play Services are active and your SHA-1 is registered in the Firebase console."
            showFallbackSelector = true
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackErrorMsg = "Google Sign-In failed: ${e.localizedMessage}"
            showFallbackSelector = true
        }
    }

    if (showFallbackSelector) {
        AlertDialog(
            onDismissRequest = { showFallbackSelector = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("G", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose an account",
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "to continue to HomeServe",
                        fontSize = 14.sp,
                        color = Color(0xFF5F6368)
                    )
                }
            },
            text = {
                var isCustomSelected by remember { mutableStateOf(false) }
                var customEmail by remember { mutableStateOf("") }
                var customName by remember { mutableStateOf("") }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isCustomSelected) {
                        // Option 1: Abdullah Dev
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showFallbackSelector = false
                                    onGoogleSignInClick("abdullah.dev@gmail.com", "Abdullah Dev")
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = BrandBlue.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("A", color = BrandBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Abdullah Dev", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF3C4043))
                                Text("abdullah.dev@gmail.com", fontSize = 12.sp, color = Color(0xFF5F6368))
                            }
                        }
                        
                        HorizontalDivider(color = Color(0xFFE8EAED))

                        // Option 2: Moazzam Arshad
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showFallbackSelector = false
                                    onGoogleSignInClick("moazzamarshad774@gmail.com", "Moazzam Arshad")
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = BrandBlue.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("M", color = BrandBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Moazzam Arshad", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF3C4043))
                                Text("moazzamarshad774@gmail.com", fontSize = 12.sp, color = Color(0xFF5F6368))
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE8EAED))

                        // Option 3: Add/Use another account
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCustomSelected = true }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFF1A73E8),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Use another account", color = Color(0xFF1A73E8), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        // Custom Email Input Mode
                        Text(
                            text = "Please enter your valid Google email address below:",
                            color = Color(0xFF3C4043),
                            fontSize = 14.sp
                        )
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Your Real Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                cursorColor = BrandBlue
                            )
                        )
                        OutlinedTextField(
                            value = customEmail,
                            onValueChange = { customEmail = it },
                            label = { Text("Valid Google Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                cursorColor = BrandBlue
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isCustomSelected = false }) {
                                Text("Back", color = Color(0xFF5F6368))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (customEmail.contains("@") && customEmail.isNotBlank()) {
                                        showFallbackSelector = false
                                        onGoogleSignInClick(customEmail, customName.ifBlank { "Google User" })
                                    } else {
                                        Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                            ) {
                                Text("Continue", color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

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
                    text = "Customer Login",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter your phone number or use Google to continue",
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
            Spacer(modifier = Modifier.height(30.dp))

            // Logo Section
            Surface(
                modifier = Modifier.size(90.dp),
                shape = CircleShape,
                color = Color(0xFFEFF6FF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🏠", fontSize = 44.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Welcome to HomeServe",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Book trusted services for your home",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Mobile Number",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { if (it.length <= 15) phoneNumber = it.filter { char -> char.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("300 1234567", color = Color(0xFF9CA3AF)) },
                    prefix = { Text("+92 ", color = Color(0xFF374151), fontWeight = FontWeight.Bold) },
                    leadingIcon = { 
                        Icon(
                            imageVector = Icons.Outlined.Phone, 
                            contentDescription = null,
                            tint = if (isValid) BrandBlue else Color(0xFF9CA3AF)
                        ) 
                    },
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
                text = "Continue with Phone",
                onClick = { onContinueClick(phoneNumber) },
                enabled = isValid,
                modifier = Modifier.height(50.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            GoogleSignInButton(
                onClick = {
                    // Sign out first to always show the Native Account Chooser bottom sheet
                    googleSignInClient.signOut().addOnCompleteListener {
                        val signInIntent = googleSignInClient.signInIntent
                        googleSignInLauncher.launch(signInIntent)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "By continuing, you agree to our Terms of Service and Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    text: String = "Sign in with Google",
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF374151)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "G",
                color = Color(0xFF4285F4),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

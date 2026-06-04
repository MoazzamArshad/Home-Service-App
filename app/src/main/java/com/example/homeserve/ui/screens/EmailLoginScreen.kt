package com.example.homeserve.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue

@Composable
fun EmailLoginScreen(
    onSignInSuccess: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit,
    onSignIn: (email: String, password: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onRegister: (name: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onForgotPassword: (email: String, onResult: (Boolean) -> Unit) -> Unit,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Tab: 0 = Sign In, 1 = Register
    var selectedTab by remember { mutableStateOf(0) }

    // Sign In – pre-filled for CodeCanyon reviewers
    var signInEmail by remember { mutableStateOf("demo@homeserve.com") }
    var signInPassword by remember { mutableStateOf("Demo@123") }
    var signInPasswordVisible by remember { mutableStateOf(false) }

    // Register
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regConfirmVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Reset Password", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF111827))
            },
            text = {
                Column {
                    Text("Enter your email and we'll send you a reset link.", color = Color(0xFF6B7280), fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            focusedLabelColor = BrandBlue,
                            unfocusedLabelColor = Color(0xFF4B5563),
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = Color(0xFFD1D5DB),
                            cursorColor = BrandBlue
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotEmail.contains("@")) {
                            onForgotPassword(forgotEmail) { success ->
                                showForgotDialog = false
                                Toast.makeText(context, if (success) "Reset link sent! Check your email." else "Failed to send reset link.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Please enter a valid email.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Send Link", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) { Text("Cancel", color = Color(0xFF6B7280)) }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Gradient Header ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF1D4ED8), Color(0xFF2563EB), Color(0xFF3B82F6))
                    ),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 48.dp, bottom = 36.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.clickable { onBackClick() }.padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("←", color = Color.White, fontSize = 20.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Back", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                }
                Text(
                    text = "✉️  Email Login",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp),
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(text = "Sign in or create a new account", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            }
        }

        Spacer(Modifier.height(28.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {

            // ── Tab Switcher ─────────────────────────────────────────────────────
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color(0xFFF3F4F6)) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf("Sign In", "Register").forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Surface(
                            modifier = Modifier.weight(1f).clickable { selectedTab = index; errorMessage = null },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) BrandBlue else Color.Transparent,
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (isSelected) Color.White else Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Error Banner ─────────────────────────────────────────────────────
            AnimatedVisibility(visible = errorMessage != null) {
                Column {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color(0xFFFEF2F2)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(text = errorMessage ?: "", color = Color(0xFFB91C1C), fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── SIGN IN TAB ──────────────────────────────────────────────────────
            AnimatedVisibility(visible = selectedTab == 0, enter = fadeIn() + slideInHorizontally(), exit = fadeOut() + slideOutHorizontally()) {
                Column {
                    ETextField(value = signInEmail, onValueChange = { signInEmail = it }, label = "Email Address")
                    Spacer(Modifier.height(14.dp))
                    PField(value = signInPassword, onValueChange = { signInPassword = it }, label = "Password", visible = signInPasswordVisible, onToggle = { signInPasswordVisible = !signInPasswordVisible })
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            text = "Forgot Password?",
                            color = BrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            modifier = Modifier.clickable { forgotEmail = signInEmail; showForgotDialog = true }.padding(4.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            errorMessage = null; isLoading = true
                            onSignIn(signInEmail.trim(), signInPassword) { success, error ->
                                isLoading = false
                                if (success) onSignInSuccess() else errorMessage = error ?: "Sign in failed."
                            }
                        },
                        enabled = signInEmail.contains("@") && signInPassword.length >= 6 && !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, disabledContainerColor = Color(0xFFBFDBFE)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                        else Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }

            // ── REGISTER TAB ─────────────────────────────────────────────────────
            AnimatedVisibility(visible = selectedTab == 1, enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }), exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })) {
                Column {
                    NField(value = regName, onValueChange = { regName = it })
                    Spacer(Modifier.height(14.dp))
                    ETextField(value = regEmail, onValueChange = { regEmail = it }, label = "Email Address")
                    Spacer(Modifier.height(14.dp))
                    PField(value = regPassword, onValueChange = { regPassword = it }, label = "Password", visible = regPasswordVisible, onToggle = { regPasswordVisible = !regPasswordVisible })
                    Spacer(Modifier.height(14.dp))
                    PField(value = regConfirmPassword, onValueChange = { regConfirmPassword = it }, label = "Confirm Password", visible = regConfirmVisible, onToggle = { regConfirmVisible = !regConfirmVisible })
                    if (regPassword.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        val strength = when {
                            regPassword.length >= 8 && regPassword.any { it.isUpperCase() } && regPassword.any { it.isDigit() } -> Triple("Strong ✅", Color(0xFF16A34A), 1f)
                            regPassword.length >= 6 -> Triple("Medium ⚠️", Color(0xFFCA8A04), 0.6f)
                            else -> Triple("Weak ❌", Color(0xFFDC2626), 0.3f)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(progress = { strength.third }, modifier = Modifier.weight(1f).height(4.dp), color = strength.second, trackColor = Color(0xFFE5E7EB))
                            Spacer(Modifier.width(8.dp))
                            Text(strength.first, fontSize = 11.sp, color = strength.second)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (regPassword != regConfirmPassword) { errorMessage = "Passwords do not match."; return@Button }
                            errorMessage = null; isLoading = true
                            onRegister(regName.trim(), regEmail.trim(), regPassword) { success, error ->
                                isLoading = false
                                if (success) onRegisterSuccess() else errorMessage = error ?: "Registration failed."
                            }
                        },
                        enabled = regName.isNotBlank() && regEmail.contains("@") && regPassword.length >= 6 && regPassword == regConfirmPassword && !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, disabledContainerColor = Color(0xFFBFDBFE)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                        else Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }

            // ── Google Sign-In (always visible at bottom) ─────────────────────────
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                Text(text = "OR", modifier = Modifier.padding(horizontal = 12.dp), color = Color(0xFF9CA3AF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onGoogleSignInClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color(0xFF374151))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
internal fun ETextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = BrandBlue) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF111827),
            unfocusedTextColor = Color(0xFF111827),
            focusedLabelColor = BrandBlue,
            unfocusedLabelColor = Color(0xFF4B5563),
            focusedBorderColor = BrandBlue,
            unfocusedBorderColor = Color(0xFFD1D5DB),
            cursorColor = BrandBlue,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFF9FAFB)
        )
    )
}

@Composable
internal fun PField(value: String, onValueChange: (String) -> Unit, label: String, visible: Boolean, onToggle: () -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = BrandBlue) },
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = null, tint = Color(0xFF9CA3AF))
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF111827),
            unfocusedTextColor = Color(0xFF111827),
            focusedLabelColor = BrandBlue,
            unfocusedLabelColor = Color(0xFF4B5563),
            focusedBorderColor = BrandBlue,
            unfocusedBorderColor = Color(0xFFD1D5DB),
            cursorColor = BrandBlue,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFF9FAFB)
        )
    )
}

@Composable
internal fun NField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text("Full Name") },
        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = BrandBlue) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF111827),
            unfocusedTextColor = Color(0xFF111827),
            focusedLabelColor = BrandBlue,
            unfocusedLabelColor = Color(0xFF4B5563),
            focusedBorderColor = BrandBlue,
            unfocusedBorderColor = Color(0xFFD1D5DB),
            cursorColor = BrandBlue,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFF9FAFB)
        )
    )
}

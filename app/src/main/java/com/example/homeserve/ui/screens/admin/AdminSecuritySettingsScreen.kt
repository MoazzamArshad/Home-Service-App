package com.example.homeserve.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.AdminViewModel

@Composable
fun AdminSecuritySettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.adminSettings.collectAsState()
    val isLoading by viewModel.settingsLoading.collectAsState()
    val isSaved by viewModel.settingsSaved.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAdminSettings()
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSavedFlag()
        }
    }

    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showPasswordDaysDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Session Timeout Picker Dialog
    if (showTimeoutDialog) {
        val timeoutOptions = listOf(5, 10, 15, 30, 60, 120)
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Session Timeout", fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            },
            text = {
                Column {
                    Text(
                        "Auto-lock the panel after inactivity",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    timeoutOptions.forEach { minutes ->
                        val label = if (minutes < 60) "$minutes minutes" else "${minutes / 60} hour${if (minutes > 60) "s" else ""}"
                        val isSelected = settings.sessionTimeoutMinutes == minutes
                        Surface(
                            onClick = {
                                viewModel.saveAdminSettings(settings.copy(sessionTimeoutMinutes = minutes))
                                showTimeoutDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isSelected) Color(0xFFEFF6FF) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) BrandBlue else Color(0xFF374151)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutDialog = false }) {
                    Text("Close", color = BrandBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Password Change Interval Dialog
    if (showPasswordDaysDialog) {
        val dayOptions = listOf(30, 60, 90, 180, 365)
        AlertDialog(
            onDismissRequest = { showPasswordDaysDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Password Expiry", fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            },
            text = {
                Column {
                    Text(
                        "Require password change after this period",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    dayOptions.forEach { days ->
                        val label = if (days < 365) "$days days" else "1 year"
                        val isSelected = settings.passwordChangeDays == days
                        Surface(
                            onClick = {
                                viewModel.saveAdminSettings(settings.copy(passwordChangeDays = days))
                                showPasswordDaysDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isSelected) Color(0xFFEFF6FF) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) BrandBlue else Color(0xFF374151)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPasswordDaysDialog = false }) {
                    Text("Close", color = BrandBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
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
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Security Settings",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Protect your admin panel",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Saved banner
                AnimatedVisibility(
                    visible = isSaved,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Settings saved successfully",
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Session & Authentication
                SecSectionHeader("Session Management", Icons.Default.Timer)

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SecToggleItem(
                            title = "Auto-Lock Panel",
                            subtitle = "Lock panel after inactivity",
                            icon = Icons.Default.Lock,
                            iconColor = Color(0xFF3B82F6),
                            isChecked = settings.autoLockEnabled,
                            onCheckedChange = {
                                viewModel.saveAdminSettings(settings.copy(autoLockEnabled = it))
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                        SecNavigationItem(
                            title = "Session Timeout",
                            subtitle = "${settings.sessionTimeoutMinutes} minutes",
                            icon = Icons.Default.AvTimer,
                            iconColor = Color(0xFF6366F1),
                            onClick = { showTimeoutDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Authentication
                SecSectionHeader("Authentication", Icons.Default.Security)

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SecToggleItem(
                            title = "Two-Factor Authentication",
                            subtitle = "Extra layer of security for login",
                            icon = Icons.Default.PhonelinkLock,
                            iconColor = Color(0xFF10B981),
                            isChecked = settings.twoFactorEnabled,
                            onCheckedChange = {
                                viewModel.saveAdminSettings(settings.copy(twoFactorEnabled = it))
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                        SecToggleItem(
                            title = "Login Notifications",
                            subtitle = "Get alerted on new login attempts",
                            icon = Icons.Default.NotificationsActive,
                            iconColor = Color(0xFFF59E0B),
                            isChecked = settings.loginNotificationsEnabled,
                            onCheckedChange = {
                                viewModel.saveAdminSettings(settings.copy(loginNotificationsEnabled = it))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Password Policy
                SecSectionHeader("Password Policy", Icons.Default.Key)

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SecToggleItem(
                            title = "Require Password Change",
                            subtitle = "Force periodic password updates",
                            icon = Icons.Default.VpnKey,
                            iconColor = Color(0xFFDC2626),
                            isChecked = settings.requirePasswordChange,
                            onCheckedChange = {
                                viewModel.saveAdminSettings(settings.copy(requirePasswordChange = it))
                            }
                        )
                        if (settings.requirePasswordChange) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                            SecNavigationItem(
                                title = "Password Expiry Period",
                                subtitle = "${settings.passwordChangeDays} days",
                                icon = Icons.Default.DateRange,
                                iconColor = Color(0xFF8B5CF6),
                                onClick = { showPasswordDaysDialog = true }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Info card
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Security Note",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Changes to security settings take effect immediately. Two-factor authentication will be enforced on the next login.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun SecSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun SecToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = iconColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = Color(0xFF111827), fontSize = 15.sp)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Color(0xFF10B981),
                uncheckedTrackColor = Color(0xFFE5E7EB)
            )
        )
    }
}

@Composable
private fun SecNavigationItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = iconColor.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, color = Color(0xFF111827), fontSize = 15.sp)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = BrandBlue)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFD1D5DB))
    }
}

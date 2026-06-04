package com.example.homeserve.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
fun AdminNotificationSettingsScreen(
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

    val scrollState = rememberScrollState()

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
                        text = "Notification Settings",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Configure your alert preferences",
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

                // Booking Notifications
                NotifSectionHeader("Booking Alerts", Icons.Default.BookOnline)

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        NotifToggleItem(
                            title = "New Bookings",
                            subtitle = "Get notified when a new booking is created",
                            icon = Icons.Default.AddAlert,
                            iconColor = Color(0xFF3B82F6),
                            isChecked = settings.notifyNewBookings,
                            onCheckedChange = {
                                viewModel.saveAdminSettings(settings.copy(notifyNewBookings = it))
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                        NotifToggleItem(
                            title = "Booking Cancellations",
                            subtitle = "Alert when a booking is cancelled",
                            icon = Icons.Default.Cancel,
                            iconColor = Color(0xFFEF4444),
                            isChecked = settings.notifyBookingCancellations,
                            onCheckedChange = {
                                viewModel.saveAdminSettings(settings.copy(notifyBookingCancellations = it))
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                        NotifToggleItem(
                            title = "Payment Updates",
                            subtitle = "Notifications for payment status changes",
                            icon = Icons.Default.Payment,
                            iconColor = Color(0xFF10B981),
                            isChecked = settings.notifyPaymentUpdates,
                            onCheckedChange = {
                                viewModel.saveAdminSettings(settings.copy(notifyPaymentUpdates = it))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Provider & System Notifications
                NotifSectionHeader("System Alerts", Icons.Default.Notifications)

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        NotifToggleItem(
                            title = "Provider Registrations",
                            subtitle = "When a new provider signs up for approval",
                            icon = Icons.Default.PersonAdd,
                            iconColor = Color(0xFFF59E0B),
                            isChecked = settings.notifyProviderRegistrations,
                            onCheckedChange = {
                                viewModel.saveAdminSettings(settings.copy(notifyProviderRegistrations = it))
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                        NotifToggleItem(
                            title = "System Alerts",
                            subtitle = "Critical platform errors and warnings",
                            icon = Icons.Default.Warning,
                            iconColor = Color(0xFFDC2626),
                            isChecked = settings.notifySystemAlerts,
                            onCheckedChange = {
                                viewModel.saveAdminSettings(settings.copy(notifySystemAlerts = it))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Channels
                NotifSectionHeader("Notification Channels", Icons.Default.Campaign)

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        NotifToggleItem(
                            title = "Email Notifications",
                            subtitle = "Receive alerts via email",
                            icon = Icons.Default.Email,
                            iconColor = Color(0xFF6366F1),
                            isChecked = settings.notifyEmailEnabled,
                            onCheckedChange = {
                                viewModel.saveAdminSettings(settings.copy(notifyEmailEnabled = it))
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF3F4F6))
                        NotifToggleItem(
                            title = "SMS Alerts",
                            subtitle = "Receive critical alerts via SMS",
                            icon = Icons.Default.Sms,
                            iconColor = Color(0xFF8B5CF6),
                            isChecked = settings.notifySmsEnabled,
                            onCheckedChange = {
                                viewModel.saveAdminSettings(settings.copy(notifySmsEnabled = it))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun NotifSectionHeader(title: String, icon: ImageVector) {
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
private fun NotifToggleItem(
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

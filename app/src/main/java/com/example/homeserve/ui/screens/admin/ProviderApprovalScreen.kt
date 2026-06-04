package com.example.homeserve.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.homeserve.ui.viewmodel.AdminViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue

// Mock classes removed in favor of com.example.homeserve.data.model.Provider

@Composable
fun ProviderApprovalScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit,
    onViewDetails: (String) -> Unit
) {
    val providers by viewModel.providersList.collectAsState()
    var activeTab by remember { mutableStateOf("pending") }
    val tabs = listOf("all", "pending", "approved", "rejected")

    val filteredProviders = remember(providers, activeTab) {
        when (activeTab) {
            "all" -> providers
            "pending" -> providers.filter { !it.isApproved && !it.isRejected }
            "approved" -> providers.filter { it.isApproved }
            "rejected" -> providers.filter { it.isRejected }
            else -> emptyList()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
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
                        text = "Provider Approvals",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Review and verify new service providers",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tab ->
                val isSelected = activeTab == tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable { activeTab = tab },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) BrandBlue else Color.White,
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = tab.replaceFirstChar { it.uppercase() },
                            color = if (isSelected) Color.White else Color(0xFF6B7280),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (filteredProviders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No ${activeTab} providers found", color = Color(0xFF6B7280))
                    }
                }
            } else {
                items(filteredProviders) { provider ->
                    PendingProviderCard(
                        provider = provider,
                        onViewDetails = { onViewDetails(provider.uid) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun PendingProviderCard(provider: com.example.homeserve.data.model.Provider, onViewDetails: () -> Unit) {
    val categoryList = provider.categoryId.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (provider.name.isNotEmpty()) provider.name.first().toString() else "P",
                            color = BrandBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(provider.name, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    Text(provider.email, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                }
                val statusText = when {
                    provider.isApproved -> "APPROVED"
                    provider.isRejected -> "REJECTED"
                    else -> "PENDING"
                }
                val statusColor = when {
                    provider.isApproved -> Color(0xFFD1FAE5)
                    provider.isRejected -> Color(0xFFFEE2E2)
                    else -> Color(0xFFFFFBEB)
                }
                val textColor = when {
                    provider.isApproved -> Color(0xFF065F46)
                    provider.isRejected -> Color(0xFF991B1B)
                    else -> Color(0xFFB45309)
                }
                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${categoryList.size} categories selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B5563)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = Color(0xFF374151))
            ) {
                val buttonText = when {
                    provider.isApproved -> "View Details"
                    provider.isRejected -> "Review Application"
                    else -> "Review Application"
                }
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

package com.example.homeserve.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.data.CustomerMockData
import com.example.homeserve.ui.theme.BrandBlue

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homeserve.ui.viewmodel.CustomerViewModel

@Composable
fun HomeScreen(
    onCategoryClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val categories by viewModel.categories.collectAsState()
    val popularServices by viewModel.popularServices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Header Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = BrandBlue,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(top = 48.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Location", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            }
                            val defaultSavedAddr = com.example.homeserve.ui.data.CustomerMockData.mockAddresses.find { it.isDefault }?.address
                                ?: com.example.homeserve.ui.data.CustomerMockData.mockAddresses.firstOrNull()?.address
                            val displayAddress = userProfile?.address?.takeIf { it.isNotBlank() }
                                ?: defaultSavedAddr
                                ?: "Set Address in Profile"
                            Text(
                                text = displayAddress,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                        Surface(
                            onClick = onProfileClick,
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👤", fontSize = 20.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val displayName = userProfile?.name?.takeIf { it.isNotBlank() } ?: "Customer"
                    Text(
                        text = "Hello $displayName, what service\ndo you need today?",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 32.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search for services...", color = Color(0xFF9CA3AF)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF9CA3AF)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = BrandBlue
                        )
                    )
                }
            }
        }

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // AI Assistant Promo Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clickable { onAiAssistantClick() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = BrandBlue
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🤖", fontSize = 24.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unsure what you need?",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1E3A8A)
                            )
                            Text(
                                text = "Try diagnosing your home issues with our Smart AI!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF3B82F6),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Try AI",
                            tint = BrandBlue
                        )
                    }
                }
            }

            // Categories Grid
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Text(
                        text = "Our Services", 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111827)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val rows = categories.chunked(3)
                    rows.forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { category ->
                                CategoryItem(
                                    category = category,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onCategoryClick(category.categoryId) }
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Popular Services
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "Popular Services", 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    popularServices.take(3).forEach { service ->
                        PopularServiceCard(service, onClick = { onCategoryClick(service.categoryId) })
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CategoryItem(
    category: com.example.homeserve.data.model.Category,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(category.icon, fontSize = 28.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF374151),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PopularServiceCard(
    service: com.example.homeserve.data.model.ServiceModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🛠️", fontSize = 24.sp)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = service.name, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                    Text(text = " 4.5", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold) // hardcoded rating for now as it's not in ServiceModel
                    Text(text = "  •  From $${service.price}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF9CA3AF)
            )
        }
    }
}

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
import androidx.compose.ui.draw.clip

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
    var isAllCategoriesExpanded by remember { mutableStateOf(false) }
    var visibleCategoriesCount by remember { mutableStateOf(6) }
    
    val categories by viewModel.categories.collectAsState()
    val popularServices by viewModel.popularServices.collectAsState()
    val allServices by viewModel.allServices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val filteredServices = remember(searchQuery, allServices) {
        if (searchQuery.isBlank()) emptyList()
        else {
            allServices.filter { service ->
                service.name.lowercase().contains(searchQuery.lowercase()) ||
                service.description.lowercase().contains(searchQuery.lowercase())
            }
        }
    }

    val recommendedCategories = remember(filteredServices, searchQuery, categories) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val matchedCategoryIds = filteredServices.map { it.categoryId }.toSet()
            categories.filter { category ->
                category.name.lowercase().contains(searchQuery.lowercase()) ||
                category.categoryId in matchedCategoryIds
            }
        }
    }

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
                                val photoUrl = userProfile?.profilePhotoUrl
                                if (!photoUrl.isNullOrBlank()) {
                                    coil.compose.AsyncImage(
                                        model = photoUrl,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text("👤", fontSize = 20.sp)
                                }
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
        } else if (searchQuery.isNotEmpty()) {
            // Search Auto-suggestions section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Suggestions for \"$searchQuery\"",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF374151)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (recommendedCategories.isEmpty() && filteredServices.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🔍", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No services or categories found",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B7280)
                                )
                                Text(
                                    text = "Try typing something else like 'wire', 'leak', or 'clean'.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9CA3AF),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        if (recommendedCategories.isNotEmpty()) {
                            Text(
                                text = "Recommended Categories",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9CA3AF)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            recommendedCategories.forEach { category ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onCategoryClick(category.categoryId) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(category.icon, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = category.name,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E3A8A)
                                            )
                                            Text(
                                                text = "Tap to view all ${category.name} services",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF3B82F6)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        if (filteredServices.isNotEmpty()) {
                            Text(
                                text = "Matching Services",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9CA3AF)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            filteredServices.forEach { service ->
                                val category = categories.find { it.categoryId == service.categoryId }
                                val categoryIcon = category?.icon ?: "🛠️"
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onCategoryClick(service.categoryId) },
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF3F4F6)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(categoryIcon, fontSize = 18.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = service.name,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF111827)
                                            )
                                            Text(
                                                text = service.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF6B7280),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Our Services", 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF111827)
                        )
                        TextButton(
                            onClick = { 
                                isAllCategoriesExpanded = !isAllCategoriesExpanded
                                if (!isAllCategoriesExpanded) {
                                    visibleCategoriesCount = 6
                                }
                            }
                        ) {
                            Text(
                                text = if (isAllCategoriesExpanded) "See Less" else "See All",
                                color = BrandBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val displayedCategories = if (isAllCategoriesExpanded) {
                        categories
                    } else {
                        categories.take(visibleCategoriesCount)
                    }
                    val rows = displayedCategories.chunked(3)
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

                    if (!isAllCategoriesExpanded && categories.size > visibleCategoriesCount) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = { visibleCategoriesCount += 3 }
                            ) {
                                Text(
                                    text = "See More",
                                    color = BrandBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
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

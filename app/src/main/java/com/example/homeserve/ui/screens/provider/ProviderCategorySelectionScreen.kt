package com.example.homeserve.ui.screens.provider

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.components.PrimaryButton
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.ProviderViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.homeserve.data.NetworkUtils

data class ServiceCategory(
    val id: String,
    val name: String,
    val icon: String,
    val backgroundColor: Color
)

val categories = listOf(
    ServiceCategory("electrician", "Electrician", "⚡", Color(0xFFFFFBEB)),
    ServiceCategory("plumber", "Plumber", "🔧", Color(0xFFEFF6FF)),
    ServiceCategory("cleaning", "Cleaning", "✨", Color(0xFFFAF5FF)),
    ServiceCategory("appliance", "Appliance Repair", "🔨", Color(0xFFF0FDF4)),
    ServiceCategory("beauty", "Beauty Services", "💅", Color(0xFFFFF1F2)),
    ServiceCategory("painting", "Painting", "🎨", Color(0xFFFFF7ED))
)

@Composable
fun ProviderCategorySelectionScreen(
    viewModel: ProviderViewModel,
    onContinueClick: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val profileState by viewModel.providerProfile.collectAsState()
    var selectedCategories by remember {
        mutableStateOf(
            profileState?.categoryId
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: emptySet()
        )
    }

    LaunchedEffect(profileState) {
        profileState?.let { profile ->
            if (selectedCategories.isEmpty()) {
                selectedCategories = profile.categoryId
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
        }
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
                    text = "Select Service Categories",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Choose the services you can provide",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategories.contains(category.id)
                    CategoryItem(
                        category = category,
                        isSelected = isSelected,
                        onClick = {
                            selectedCategories = if (isSelected) {
                                selectedCategories - category.id
                            } else {
                                selectedCategories + category.id
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check, 
                        contentDescription = null, 
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Selected: ${selectedCategories.size} categories",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E40AF)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                PrimaryButton(
                    text = "Save & Continue",
                    onClick = {
                        if (NetworkUtils.isNetworkAvailable(context)) {
                            viewModel.saveProviderProfileWithCategories(context, selectedCategories.toList()) {
                                onContinueClick(selectedCategories.toList())
                            }
                        } else {
                            Toast.makeText(context, "No internet connection. Please connect to the internet to complete your registration.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = selectedCategories.isNotEmpty(),
                    modifier = Modifier.height(54.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: ServiceCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f)
    val borderColor by animateColorAsState(if (isSelected) BrandBlue else Color.Transparent)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .scale(scale)
            .clickable(onClick = onClick)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = category.backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isSelected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp),
                    shape = CircleShape,
                    color = BrandBlue
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = category.icon, fontSize = 40.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF111827)
                )
            }
        }
    }
}

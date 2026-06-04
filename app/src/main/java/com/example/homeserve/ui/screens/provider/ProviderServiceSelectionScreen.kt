package com.example.homeserve.ui.screens.provider

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.data.NetworkUtils
import com.example.homeserve.ui.components.PrimaryButton
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.ProviderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderServiceSelectionScreen(
    viewModel: ProviderViewModel,
    categoryIds: List<String>,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val profileState by viewModel.providerProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val servicesForCategories by viewModel.servicesForCategories.collectAsState()

    var selectedServices by remember {
        mutableStateOf(
            profileState?.selectedServiceIds
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: emptySet()
        )
    }

    LaunchedEffect(profileState) {
        profileState?.let { profile ->
            if (selectedServices.isEmpty()) {
                selectedServices = profile.selectedServiceIds
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
        }
    }

    LaunchedEffect(categoryIds) {
        viewModel.loadServicesForCategories(categoryIds)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Premium Header with Wizard Step Indicator
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Select Services",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                    
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Step 3 of 3",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pick the specific tasks you handle in your selected categories",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        if (isLoading && servicesForCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                categoryIds.forEach { categoryId ->
                    val services = servicesForCategories[categoryId] ?: emptyList()
                    val categoryName = getCategoryFriendlyName(categoryId)
                    val categoryColor = getCategoryColor(categoryId)
                    val themeColor = getCategoryThemeColor(categoryId)

                    if (services.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Category Header Block
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = categoryColor,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = categoryName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = themeColor,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }

                                    val allChecked = services.all { selectedServices.contains(it.serviceId) }
                                    TextButton(
                                        onClick = {
                                            selectedServices = if (allChecked) {
                                                selectedServices - services.map { it.serviceId }.toSet()
                                            } else {
                                                selectedServices + services.map { it.serviceId }.toSet()
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = if (allChecked) "Deselect All" else "Select All",
                                            color = themeColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Service Items List
                                services.forEach { service ->
                                    val isChecked = selectedServices.contains(service.serviceId)
                                    
                                    val rowBgColor by animateColorAsState(
                                        if (isChecked) categoryColor else Color(0xFFF9FAFB)
                                    )
                                    val rowBorderColor by animateColorAsState(
                                        if (isChecked) themeColor.copy(alpha = 0.4f) else Color(0xFFE5E7EB)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(rowBgColor)
                                            .clickable {
                                                selectedServices = if (isChecked) {
                                                    selectedServices - service.serviceId
                                                } else {
                                                    selectedServices + service.serviceId
                                                }
                                            }
                                            .border(
                                                width = 1.dp,
                                                color = rowBorderColor,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CustomCheckIndicator(
                                            checked = isChecked,
                                            themeColor = themeColor
                                        )
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = service.name,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF111827),
                                                fontSize = 15.sp
                                            )
                                            if (service.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = service.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF6B7280),
                                                    lineHeight = 16.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Standard Rate: $${service.price}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                ),
                                                color = themeColor
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Finish Button Panel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                PrimaryButton(
                    text = "Save & Finish Setup",
                    onClick = {
                        if (NetworkUtils.isNetworkAvailable(context)) {
                            viewModel.saveProviderProfileWithCategoriesAndServices(
                                context = context,
                                selectedCategories = categoryIds,
                                selectedServices = selectedServices.toList()
                            ) {
                                onComplete()
                            }
                        } else {
                            Toast.makeText(context, "No internet connection. Please connect to the internet to complete your registration.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = selectedServices.isNotEmpty(),
                    modifier = Modifier.height(54.dp)
                )
            }
        }
    }

    if (isLoading && servicesForCategories.isNotEmpty()) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandBlue)
            }
        }
    }
}

@Composable
fun CustomCheckIndicator(
    checked: Boolean,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (checked) 1f else 0.85f, label = "checkScale")
    val bgColor by animateColorAsState(if (checked) themeColor else Color.Transparent, label = "checkBg")
    val borderColor by animateColorAsState(if (checked) themeColor else Color(0xFFD1D5DB), label = "checkBorder")

    Box(
        modifier = modifier
            .size(24.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun getCategoryFriendlyName(categoryId: String): String {
    return when (categoryId) {
        "electrician" -> "Electrician ⚡"
        "plumber" -> "Plumber 🔧"
        "cleaning" -> "Cleaning ✨"
        "appliance" -> "Appliance Repair 🔨"
        "beauty" -> "Beauty Services 💅"
        "painting" -> "Painting 🎨"
        else -> categoryId.replaceFirstChar { it.uppercase() }
    }
}

private fun getCategoryColor(categoryId: String): Color {
    return when (categoryId) {
        "electrician" -> Color(0xFFFFFBEB)
        "plumber" -> Color(0xFFEFF6FF)
        "cleaning" -> Color(0xFFFAF5FF)
        "appliance" -> Color(0xFFF0FDF4)
        "beauty" -> Color(0xFFFFF1F2)
        "painting" -> Color(0xFFFFF7ED)
        else -> Color(0xFFF9FAFB)
    }
}

private fun getCategoryThemeColor(categoryId: String): Color {
    return when (categoryId) {
        "electrician" -> Color(0xFFD97706) // Softer Amber
        "plumber" -> Color(0xFF2563EB)      // Soft Blue
        "cleaning" -> Color(0xFF7C3AED)     // Soft Violet
        "appliance" -> Color(0xFF16A34A)    // Soft Green
        "beauty" -> Color(0xFFDB2777)       // Soft Pink
        "painting" -> Color(0xFFEA580C)     // Soft Orange
        else -> BrandBlue
    }
}

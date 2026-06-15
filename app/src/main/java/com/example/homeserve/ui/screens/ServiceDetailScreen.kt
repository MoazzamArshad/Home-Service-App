package com.example.homeserve.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.components.PrimaryButton
import com.example.homeserve.ui.data.CustomerMockData
import com.example.homeserve.ui.theme.BrandBlue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.horizontalScroll

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homeserve.ui.viewmodel.CustomerViewModel

@Composable
fun ServiceDetailScreen(
    serviceId: String,
    onBackClick: () -> Unit,
    onBookNowClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerViewModel = viewModel()
) {
    val context = LocalContext.current
    val allBookings by viewModel.userBookings.collectAsState()
    val activeBookings = remember(allBookings) {
        allBookings.filter { it.status.lowercase() in listOf("pending", "accepted", "in_progress") }
    }
    val serviceReviews by viewModel.serviceReviews.collectAsState()
    var showReviewsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(serviceId) {
        viewModel.fetchReviewsForService(serviceId)
    }

    val avgRating = remember(serviceReviews) {
        if (serviceReviews.isNotEmpty()) {
            serviceReviews.sumOf { it.rating }.toDouble() / serviceReviews.size
        } else {
            4.8
        }
    }
    val reviewCount = remember(serviceReviews) { serviceReviews.size }

    // Attempt to find the service in already loaded lists (categoryServices or popularServices)
    val categoryServices by viewModel.categoryServices.collectAsState()
    val popularServices by viewModel.popularServices.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val service = categoryServices.find { it.serviceId == serviceId } 
        ?: popularServices.find { it.serviceId == serviceId }
        
    val category = categories.find { it.categoryId == service?.categoryId }

    if (service == null) return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Header with Icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BrandBlue,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 48.dp, bottom = 40.dp, start = 16.dp, end = 24.dp)
        ) {
            Column {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(category?.icon ?: "🛠️", fontSize = 42.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = service.name,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                            Text(
                                text = " ${String.format(java.util.Locale.US, "%.1f", avgRating)} ($reviewCount review${if (reviewCount == 1) "" else "s"})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Price card removed

            // Description
            Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = service.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4B5563),
                lineHeight = 22.sp
            )
            Text(
                text = "Our trained professionals will arrive at your doorstep with all necessary tools and equipment. We ensure quality service and customer satisfaction.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4B5563),
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Included Section
            Text("What's Included", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
            Spacer(modifier = Modifier.height(12.dp))
            
            IncludedItem("Background verified professionals")
            IncludedItem("High-quality equipment used")
            IncludedItem("30-day post-service warranty")
            IncludedItem("COVID-19 safety protocols followed")

            Spacer(modifier = Modifier.height(24.dp))

            // Reviews Card
            Surface(
                onClick = { showReviewsDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color(0xFFFFF7ED)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ratings & Reviews", fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        Text(
                            text = if (reviewCount > 0) "Check feedback from $reviewCount user${if (reviewCount == 1) "" else "s"}" else "No reviews yet. Be the first to rate!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFD1D5DB))
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Bottom Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 16.dp,
            color = Color.White
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                PrimaryButton(
                    text = "Book This Service",
                    onClick = {
                        if (activeBookings.size >= 3) {
                            Toast.makeText(context, "You can book up to 3 services at a time. Please complete or cancel your active bookings first.", Toast.LENGTH_LONG).show()
                        } else {
                            onBookNowClick(service.serviceId)
                        }
                    }
                )
            }
        }
    }

    if (showReviewsDialog) {
        AlertDialog(
            onDismissRequest = { showReviewsDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reviews & Ratings", fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    TextButton(onClick = { showReviewsDialog = false }) {
                        Text("Close", color = BrandBlue, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (serviceReviews.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No reviews yet. Book this service and be the first to leave a review!", color = Color(0xFF6B7280), textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(serviceReviews) { reviewItem ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = reviewItem.customerName.ifBlank { "Anonymous Customer" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF111827)
                                            )
                                            Row {
                                                (1..5).forEach { starIndex ->
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = if (starIndex <= reviewItem.rating) Color(0xFFF59E0B) else Color(0xFFD1D5DB),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        if (reviewItem.reviewTags.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            androidx.compose.foundation.layout.Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                            ) {
                                                reviewItem.reviewTags.forEach { tag ->
                                                    Surface(
                                                        color = BrandBlue.copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = tag,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BrandBlue
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (reviewItem.reviewText.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = reviewItem.reviewText,
                                                fontSize = 13.sp,
                                                color = Color(0xFF4B5563)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun IncludedItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle, 
            contentDescription = null, 
            tint = Color(0xFF10B981),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4B5563))
    }
}

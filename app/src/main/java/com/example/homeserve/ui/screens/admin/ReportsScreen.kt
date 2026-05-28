package com.example.homeserve.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.AdminViewModel
import java.util.Calendar

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
    val bookings by viewModel.bookingsList.collectAsState()
    var dateRange by remember { mutableStateOf("Month") }
    val ranges = listOf("Today", "Week", "Month", "Year")

    val filteredBookings = remember(bookings, dateRange) {
        val now = Calendar.getInstance()
        val limit = Calendar.getInstance()
        when (dateRange) {
            "Today" -> limit.add(Calendar.DAY_OF_YEAR, -1)
            "Week" -> limit.add(Calendar.DAY_OF_YEAR, -7)
            "Month" -> limit.add(Calendar.DAY_OF_YEAR, -30)
            "Year" -> limit.add(Calendar.DAY_OF_YEAR, -365)
        }
        bookings.filter { booking ->
            val bookingTime = booking.scheduledDate.toDate().time
            bookingTime >= limit.timeInMillis && bookingTime <= now.timeInMillis
        }
    }

    val totalRevenue = remember(filteredBookings) {
        filteredBookings.filter { it.status == "completed" }.sumOf { it.totalAmount }
    }

    val totalBookingsCount = remember(filteredBookings) {
        filteredBookings.size
    }

    val commission = remember(totalRevenue) {
        (totalRevenue * 0.15).toInt()
    }

    val categoryCounts = remember(filteredBookings) {
        val counts = mutableMapOf<String, Int>()
        filteredBookings.forEach { booking ->
            val catName = booking.categoryId.replaceFirstChar { it.uppercase() }
            if (catName.isNotEmpty()) {
                counts[catName] = (counts[catName] ?: 0) + 1
            }
        }
        counts.toList().sortedByDescending { it.second }
    }

    val maxJobs = remember(categoryCounts) {
        categoryCounts.maxOfOrNull { it.second } ?: 1
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
                        text = "System Reports",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Platform performance analytics",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Date Range Selector
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ranges.forEach { range ->
                        val isSelected = dateRange == range
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (isSelected) BrandBlue else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { dateRange = range },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = range,
                                color = if (isSelected) Color.White else Color(0xFF6B7280),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Financial Summary
            Text(
                text = "Financial Overview",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Total Revenue", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4B5563))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "Rs. $totalRevenue",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Text("12%", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bookings", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4B5563))
                            Text("$totalBookingsCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF111827))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Commission", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4B5563))
                            Text("Rs. $commission", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF111827))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Service Performance
            Text(
                text = "Service Performance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (categoryCounts.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No jobs in this date range", color = Color(0xFF6B7280))
                    }
                }
            } else {
                categoryCounts.forEach { (catName, jobCount) ->
                    val color = when (catName.lowercase()) {
                        "electrician" -> Color(0xFF3B82F6)
                        "cleaning" -> Color(0xFF8B5CF6)
                        "plumbing", "plumber" -> Color(0xFF10B981)
                        else -> BrandBlue
                    }
                    PerformanceCard(catName, jobCount, jobCount.toFloat() / maxJobs, color)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PerformanceCard(name: String, count: Int, progress: Float, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = name, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                Text(text = "$count Jobs", color = Color(0xFF6B7280), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
        }
    }
}

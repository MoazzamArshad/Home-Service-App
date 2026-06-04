package com.example.homeserve.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.AdminViewModel
import com.example.homeserve.data.model.SupportTicket
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
    val tickets by viewModel.supportTickets.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("all") }
    var selectedRoleFilter by remember { mutableStateOf("all") }

    val filterTabs = listOf("all", "open", "in-progress", "resolved")

    val filteredTickets = tickets.filter { ticket ->
        val matchesSearch = ticket.customerName.contains(searchQuery, ignoreCase = true) ||
                ticket.subject.contains(searchQuery, ignoreCase = true) ||
                ticket.customerPhone.contains(searchQuery, ignoreCase = true)
        val matchesStatus = selectedStatus == "all" || ticket.status.lowercase() == selectedStatus
        val matchesRole = selectedRoleFilter == "all" || ticket.submitterRole.lowercase() == selectedRoleFilter
        matchesSearch && matchesStatus && matchesRole
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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Customer Support",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Manage help desk tickets",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by customer, phone or subject...", color = Color(0xFF9CA3AF)) },
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

        // Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterTabs.forEach { status ->
                val isSelected = selectedStatus == status
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedStatus = status },
                    label = { Text(status.replace("-", " ").replaceFirstChar { it.uppercase() }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBlue,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = Color(0xFF6B7280)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color(0xFFE5E7EB),
                        selectedBorderColor = BrandBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Submitter Role Filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Submitter Type:",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(end = 4.dp)
            )
            
            val roleFilters = listOf("all" to "All", "customer" to "Customers", "provider" to "Providers")
            roleFilters.forEach { (filterVal, label) ->
                val isSelected = selectedRoleFilter == filterVal
                
                Surface(
                    onClick = { selectedRoleFilter = filterVal },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) BrandBlue.copy(alpha = 0.12f) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) BrandBlue else Color(0xFFE5E7EB)
                    ),
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) BrandBlue else Color(0xFF6B7280),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (filteredTickets.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFD1D5DB))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No tickets found", color = Color(0xFF6B7280))
                        }
                    }
                }
            } else {
                items(filteredTickets) { ticket ->
                    SupportTicketCard(
                        ticket = ticket,
                        onStatusChange = { newStatus ->
                            viewModel.updateTicketStatus(ticket.id, newStatus)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SupportTicketCard(
    ticket: SupportTicket,
    onStatusChange: (String) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row: Subject, Customer Name, and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ticket.subject,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Submitter Details: Name, Role Badge, Phone
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = ticket.customerName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF374151)
                        )
                        
                        // Submitter Role Badge
                        val roleText = ticket.submitterRole.lowercase().replaceFirstChar { it.uppercase() }
                        val roleBgColor = if (ticket.submitterRole.lowercase() == "provider") Color(0xFFEEF2FF) else Color(0xFFE0F2FE)
                        val roleTextColor = if (ticket.submitterRole.lowercase() == "provider") Color(0xFF4F46E5) else Color(0xFF0284C7)
                        
                        Surface(
                            color = roleBgColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = roleText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = roleTextColor
                            )
                        }
                    }
                    
                    if (ticket.customerPhone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = ticket.customerPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
                
                // Clickable Status Badge with Dropdown
                Box {
                    TicketStatusBadge(
                        status = ticket.status,
                        onClick = { showStatusMenu = true }
                    )
                    
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        listOf("open", "in-progress", "resolved").forEach { statusOption ->
                            val label = if (statusOption == "in-progress") "In Progress" else statusOption.replaceFirstChar { it.uppercase() }
                            val color = when (statusOption) {
                                "open" -> Color(0xFFDC2626)
                                "in-progress" -> Color(0xFFCA8A04)
                                "resolved" -> Color(0xFF059669)
                                else -> Color(0xFF6B7280)
                            }
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = label,
                                        color = color,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    onStatusChange(statusOption)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = ticket.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4B5563)
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF3F4F6))
            Spacer(modifier = Modifier.height(16.dp))

            // Footer Row: Timestamp & Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatTimestamp(ticket.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9CA3AF)
                    )
                }
                
                val priorityColor = when (ticket.priority.lowercase()) {
                    "high" -> Color(0xFFDC2626)
                    "medium" -> Color(0xFFD97706)
                    else -> Color(0xFF059669)
                }
                
                Surface(
                    color = priorityColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${ticket.priority.uppercase()} PRIORITY",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor
                    )
                }
            }
        }
    }
}

@Composable
fun TicketStatusBadge(
    status: String,
    onClick: () -> Unit
) {
    val (color, bgColor) = when (status.lowercase()) {
        "open" -> Color(0xFFDC2626) to Color(0xFFFEF2F2)
        "in-progress" -> Color(0xFFCA8A04) to Color(0xFFFFFBEB)
        "resolved" -> Color(0xFF059669) to Color(0xFFECFDF5)
        else -> Color(0xFF6B7280) to Color(0xFFF3F4F6)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (status.lowercase() == "in-progress") "In Progress" else status.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Change Status",
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
    return sdf.format(date)
}

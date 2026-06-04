package com.example.homeserve.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.data.model.AppNotification
import com.example.homeserve.data.model.ChatThread
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InboxScreen(
    chatThreads: List<ChatThread>,
    notifications: List<AppNotification>,
    onChatThreadClick: (ChatThread) -> Unit,
    onNotificationClick: (AppNotification) -> Unit,
    onDeleteNotification: (AppNotification) -> Unit,
    onClearAllNotifications: () -> Unit,
    onDeleteChatThread: (ChatThread) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var threadToDelete by remember { mutableStateOf<ChatThread?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    // Chat Thread deletion confirmation dialog
    if (threadToDelete != null) {
        AlertDialog(
            onDismissRequest = { threadToDelete = null },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Delete Conversation?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to hide this conversation from your inbox? This won't delete the messages for the other user.",
                    color = Color(0xFF4B5563)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteChatThread(threadToDelete!!)
                        threadToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { threadToDelete = null }) {
                    Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Clear All Notifications confirmation dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Clear All Notifications?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all notifications? This action cannot be undone.",
                    color = Color(0xFF4B5563)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllNotifications()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Premium Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BrandBlue,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 48.dp, bottom = 20.dp, start = 16.dp, end = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Inbox",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Your messages and updates in one place",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Custom Tab Selector (Chats & Notifications)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = BrandBlue,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = BrandBlue
                )
            }
        ) {
            val unreadMessagesCount = chatThreads.count { it.unreadCount > 0 }
            val unreadNotificationsCount = notifications.count { !it.read }

            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Chats",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                        if (unreadMessagesCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = Color(0xFFEF4444)) {
                                Text(
                                    text = unreadMessagesCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Notifications",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                        if (unreadNotificationsCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = Color(0xFFEF4444)) {
                                Text(
                                    text = unreadNotificationsCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (selectedTab == 0) {
                ChatsTabContent(
                    chatThreads = chatThreads,
                    onChatThreadClick = onChatThreadClick,
                    onDeleteChatThread = { threadToDelete = it }
                )
            } else {
                NotificationsTabContent(
                    notifications = notifications,
                    onNotificationClick = onNotificationClick,
                    onDeleteNotification = onDeleteNotification,
                    onClearAllClick = { showClearAllDialog = true }
                )
            }
        }
    }
}

@Composable
private fun ChatsTabContent(
    chatThreads: List<ChatThread>,
    onChatThreadClick: (ChatThread) -> Unit,
    onDeleteChatThread: (ChatThread) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (chatThreads.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFD1D5DB)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No active chats yet", color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Your messages will appear here", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Tip: Long-press a conversation to delete it.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                )
            }
            items(chatThreads) { thread ->
                ChatThreadCard(
                    thread = thread,
                    onClick = { onChatThreadClick(thread) },
                    onLongClick = { onDeleteChatThread(thread) }
                )
            }
        }
    }
}

@Composable
private fun NotificationsTabContent(
    notifications: List<AppNotification>,
    onNotificationClick: (AppNotification) -> Unit,
    onDeleteNotification: (AppNotification) -> Unit,
    onClearAllClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (notifications.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onClearAllClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clear All",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (notifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFFD1D5DB)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No notifications yet", color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("System updates and alerts appear here", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(notifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = { onNotificationClick(notification) },
                        onDeleteClick = { onDeleteNotification(notification) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatThreadCard(
    thread: ChatThread,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val formattedTime = formatChatTime(thread.lastMessageTimestamp)
    val hasUnread = thread.unreadCount > 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (hasUnread) Color(0xFFEFF6FF) else Color.White,
        border = if (hasUnread) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.15f)) else null,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Circle
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (hasUnread) Color.White else Color(0xFFEFF6FF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val initials = thread.otherPartyName.take(1).uppercase()
                    if (thread.otherPartyPhotoUrl.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = thread.otherPartyPhotoUrl,
                            contentDescription = thread.otherPartyName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.otherPartyName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = Color(0xFF111827),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasUnread) BrandBlue else Color(0xFF9CA3AF)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.lastMessageText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasUnread) Color(0xFF1F2937) else Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.size(8.dp),
                            color = BrandBlue,
                            shape = CircleShape
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val date = notification.timestamp.toDate()
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val formattedTime = sdf.format(date)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (notification.read) Color.White else Color(0xFFEFF6FF),
        border = if (!notification.read) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.1f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (notification.read) Color(0xFFF3F4F6) else Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (notification.read) Color(0xFF9CA3AF) else BrandBlue
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111827),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (!notification.read) {
                            Surface(
                                modifier = Modifier.size(8.dp),
                                color = BrandBlue,
                                shape = CircleShape
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Notification",
                                tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B5563),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}

private fun formatChatTime(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val now = Date()
    
    val cal1 = java.util.Calendar.getInstance().apply { time = date }
    val cal2 = java.util.Calendar.getInstance().apply { time = now }
    
    val isToday = cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                  cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
                  
    val isYesterday = cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                      cal2.get(java.util.Calendar.DAY_OF_YEAR) - cal1.get(java.util.Calendar.DAY_OF_YEAR) == 1
                      
    return when {
        isToday -> {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
        }
        isYesterday -> {
            "Yesterday"
        }
        else -> {
            SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
        }
    }
}

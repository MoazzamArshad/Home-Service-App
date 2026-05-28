package com.example.homeserve.ui.screens

import com.example.homeserve.data.model.ChatMessage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    bookingId: String,
    senderRole: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel()
) {
    LaunchedEffect(bookingId, senderRole) {
        viewModel.initChat(bookingId, senderRole)
    }

    val messages by viewModel.messages.collectAsState()
    val bookingInfo by viewModel.bookingInfo.collectAsState()
    val otherPartyPhotoUrl by viewModel.otherPartyPhotoUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val otherPartyName = remember(bookingInfo, senderRole) {
        val booking = bookingInfo ?: return@remember "Loading..."
        if (senderRole == "customer") {
            if (booking.providerName.isNotBlank()) booking.providerName else "Provider"
        } else {
            if (booking.customerName.isNotBlank()) booking.customerName else "Customer"
        }
    }

    val otherPartyAvatarText = remember(otherPartyName) {
        if (otherPartyName.isBlank() || otherPartyName == "Loading...") "💬"
        else otherPartyName.take(1).uppercase(Locale.getDefault())
    }

    val serviceDetail = remember(bookingInfo) {
        bookingInfo?.serviceName ?: "Home Service"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // App Bar / Chat Header
        Surface(
            color = BrandBlue,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 16.dp, start = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Profile Avatar Bubble
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (otherPartyPhotoUrl.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = otherPartyPhotoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = otherPartyAvatarText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and Active details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = otherPartyName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = serviceDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Status Indicator Dot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Live",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Messages Feed
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading && messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandBlue)
                }
            } else if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "👋",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Start chatting with $otherPartyName!",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = "Ask questions, give directions, or send instructions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.messageId.ifEmpty { it.timestamp.seconds.toString() + Math.random() } }) { message ->
                        val isCurrentUser = message.senderRole == senderRole
                        MessageBubble(
                            message = message,
                            isCurrentUser = isCurrentUser
                        )
                    }
                }
            }
        }

        // Message Input Dock
        Surface(
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Write a message...", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827),
                        focusedContainerColor = Color(0xFFF3F4F6),
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = BrandBlue
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                FloatingActionButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(textInput.trim())
                            textInput = ""
                        }
                    },
                    shape = CircleShape,
                    containerColor = BrandBlue,
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean
) {
    val bubbleColor = if (isCurrentUser) BrandBlue else Color.White
    val textColor = if (isCurrentUser) Color.White else Color(0xFF111827)
    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
    val shape = if (isCurrentUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    val timeString = remember(message.timestamp) {
        try {
            val date = message.timestamp.toDate()
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            format.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = if (isCurrentUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = textColor
                )
                
                if (timeString.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeString,
                        fontSize = 9.sp,
                        color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color(0xFF9CA3AF),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

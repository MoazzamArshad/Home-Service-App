package com.example.homeserve.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import com.example.homeserve.ui.viewmodel.AiAssistantViewModel
import com.example.homeserve.ui.viewmodel.AiMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    onBackClick: () -> Unit,
    onBookCategoryClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AiAssistantViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to bottom when messages list size changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // App Bar / Header
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

                // AI Avatar Icon Bubble
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "🤖",
                            fontSize = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // AI Header Name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HomeServe AI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Smart Issues Diagnoser",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Active Badge
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Online",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        val presets = listOf(
            Triple("💧 Tap Leakage", "🔧 Plumber", "My kitchen faucet is leaking and spraying water everywhere"),
            Triple("⚡ Switch Sparks", "🔌 Electrician", "When I plug in my phone, the socket sparks and the power trips"),
            Triple("❄️ AC Warm Air", "🔨 Appliance", "My AC is running but it's only blowing warm air and making a clicking noise"),
            Triple("✨ Deep Clean", "🧹 Cleaning", "I need a thorough cleaning of my entire house including the kitchen and bathrooms")
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "⚡ RAPID DIAGNOSTICS PRESETS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val (label, category, textToSend) = preset
                Surface(
                    onClick = {
                        if (!isLoading) {
                            viewModel.sendMessage(textToSend)
                        }
                    },
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF374151))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = category,
                            fontSize = 10.sp,
                            color = BrandBlue,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(BrandBlue.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Messages Box / Chat Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    AiMessageBubble(
                        message = message,
                        onBookCategoryClick = onBookCategoryClick
                    )
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = BrandBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HomeServe AI is thinking...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Input Dock
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
                    placeholder = { Text("Ask about any home problem (e.g. wet wall, broken spark)", color = Color(0xFF9CA3AF)) },
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
                        if (textInput.isNotBlank() && !isLoading) {
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
                        contentDescription = "Send to AI",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: AiMessage,
    onBookCategoryClick: (String) -> Unit
) {
    val bubbleColor = if (message.isUser) BrandBlue else Color.White
    val textColor = if (message.isUser) Color.White else Color(0xFF111827)
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    val timeString = remember(message.timestamp) {
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        format.format(Date(message.timestamp))
    }

    // Parse RECOMMEND tag if it exists
    val rawText = message.text
    val recommendPattern = "\\[RECOMMEND:\\s*([a-zA-Z0-9_-]+)\\]".toRegex()
    val matchResult = recommendPattern.find(rawText)
    
    val cleanText = if (matchResult != null) {
        rawText.replace(recommendPattern, "").trim()
    } else {
        rawText
    }
    
    val categoryId = matchResult?.groupValues?.get(1)?.lowercase()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = if (message.isUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = cleanText,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = textColor
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeString,
                    fontSize = 9.sp,
                    color = if (message.isUser) Color.White.copy(alpha = 0.7f) else Color(0xFF9CA3AF),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        if (categoryId != null && !message.isUser) {
            val categoryLabel = when (categoryId) {
                "electrician" -> "Electrician"
                "plumber" -> "Plumber"
                "cleaning" -> "Cleaning"
                "appliance" -> "Appliance Repair"
                "beauty" -> "Beauty Services"
                "painting" -> "Painting"
                else -> categoryId.replaceFirstChar { it.uppercase() }
            }
            
            val categoryEmoji = when (categoryId) {
                "electrician" -> "⚡"
                "plumber" -> "🔧"
                "cleaning" -> "✨"
                "appliance" -> "🔨"
                "beauty" -> "💅"
                "painting" -> "🎨"
                else -> "🛠️"
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clickable { onBookCategoryClick(categoryId) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = BrandBlue.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(categoryEmoji, fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "$categoryLabel Recommended",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF111827)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onBookCategoryClick(categoryId) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "⚡ Browse $categoryLabel Services", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 12.sp, 
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

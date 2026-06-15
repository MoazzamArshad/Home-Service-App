package com.example.homeserve.ui.screens

import com.example.homeserve.data.model.ChatMessage
import com.example.homeserve.data.model.Booking

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
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
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import java.io.File
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.draw.clip

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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var showPriceUpdateDialog by remember { mutableStateOf(false) }

    // Audio recording state
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var recordingTimer by remember { mutableStateOf(0) }
    var isUploadingAudio by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploadingImage = true
            scope.launch {
                val storageRepo = com.example.homeserve.data.StorageRepository()
                val url = storageRepo.uploadFile(context, uri, "chat_images")
                isUploadingImage = false
                if (url != null) {
                    viewModel.sendImageMessage(url)
                } else {
                    Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun startRecording() {
        try {
            val file = File.createTempFile("homeserve_chat_voice_", ".m4a", context.cacheDir)
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            recorder.apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            recordedFile = file
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to start audio recording", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTimer = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordingTimer++
                if (recordingTimer >= 60) {
                    try {
                        mediaRecorder?.stop()
                        mediaRecorder?.release()
                    } catch (e: Exception) {}
                    mediaRecorder = null
                    isRecording = false
                    
                    val fileToUpload = recordedFile
                    val duration = recordingTimer
                    if (fileToUpload != null && fileToUpload.exists() && duration > 0) {
                        isUploadingAudio = true
                        val storageRepo = com.example.homeserve.data.StorageRepository()
                        val uri = android.net.Uri.fromFile(fileToUpload)
                        val url = storageRepo.uploadFile(context, uri, "chat_audios")
                        isUploadingAudio = false
                        if (url != null) {
                            viewModel.sendVoiceMessage(url, duration)
                        } else {
                            Toast.makeText(context, "Failed to upload voice note", Toast.LENGTH_SHORT).show()
                        }
                    }
                    recordedFile = null
                    Toast.makeText(context, "Recording limit reached (60 seconds)", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val requestAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.release()
            } catch (e: Exception) {}
        }
    }

    // Track typing state with 2-second debounce
    LaunchedEffect(textInput) {
        if (textInput.isNotEmpty()) {
            viewModel.setTyping(true)
            kotlinx.coroutines.delay(2000)
            viewModel.setTyping(false)
        } else {
            viewModel.setTyping(false)
        }
    }

    // Reset typing state on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setTyping(false)
        }
    }

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

    val isOtherTyping = remember(bookingInfo, senderRole) {
        val booking = bookingInfo ?: return@remember false
        if (senderRole == "customer") {
            booking.providerTyping
        } else {
            booking.customerTyping
        }
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
                    val priceText = bookingInfo?.let { "Rs. ${it.totalAmount}" } ?: ""
                    Text(
                        text = if (priceText.isNotEmpty()) "$serviceDetail  •  $priceText" else serviceDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Edit Price Option for Providers
                if (senderRole == "provider" && bookingInfo != null) {
                    IconButton(onClick = { showPriceUpdateDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Update Price",
                            tint = Color.White
                        )
                    }
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
                            isCurrentUser = isCurrentUser,
                            booking = bookingInfo
                        )
                    }
                }
            }
        }

        if (isOtherTyping) {
            Text(
                text = "typing...",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color(0xFF6B7280)
                ),
                modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            )
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
                if (isRecording) {
                    IconButton(
                        onClick = {
                            try {
                                mediaRecorder?.stop()
                                mediaRecorder?.release()
                            } catch (e: Exception) {}
                            mediaRecorder = null
                            isRecording = false
                            recordedFile?.delete()
                            recordedFile = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Cancel recording",
                            tint = Color(0xFFDC2626)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Recording: ${recordingTimer}s / 60s max",
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    FloatingActionButton(
                        onClick = {
                            try {
                                mediaRecorder?.stop()
                                mediaRecorder?.release()
                            } catch (e: Exception) {}
                            mediaRecorder = null
                            isRecording = false
                            
                            val fileToUpload = recordedFile
                            val duration = recordingTimer
                            if (fileToUpload != null && fileToUpload.exists() && duration > 0) {
                                isUploadingAudio = true
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    val storageRepo = com.example.homeserve.data.StorageRepository()
                                    val uri = android.net.Uri.fromFile(fileToUpload)
                                    val url = storageRepo.uploadFile(context, uri, "chat_audios")
                                    isUploadingAudio = false
                                    if (url != null) {
                                        viewModel.sendVoiceMessage(url, duration)
                                    } else {
                                        Toast.makeText(context, "Failed to upload voice note", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            recordedFile = null
                        },
                        shape = CircleShape,
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop & Send",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (isUploadingImage) {
                            CircularProgressIndicator(color = BrandBlue, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Send Image",
                                tint = BrandBlue
                            )
                        }
                    }

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

                    if (textInput.isBlank()) {
                        FloatingActionButton(
                            onClick = {
                                val checkPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                if (checkPermission == PackageManager.PERMISSION_GRANTED) {
                                    startRecording()
                                } else {
                                    requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            shape = CircleShape,
                            containerColor = BrandBlue,
                            contentColor = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (isUploadingAudio) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record Voice Note",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
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
    }

    // Price Update Dialog
    if (showPriceUpdateDialog) {
        var newPrice by remember { mutableStateOf(bookingInfo?.totalAmount?.toString() ?: "") }
        var priceError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPriceUpdateDialog = false },
            containerColor = Color.White,
            title = { Text("Update Service Price", fontWeight = FontWeight.Bold, color = Color(0xFF111827)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter the renegotiated price in PKR. Both you and the customer will see the updated amount.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4B5563)
                    )
                    OutlinedTextField(
                        value = newPrice,
                        onValueChange = { input ->
                            if (input.length <= 6) {
                                newPrice = input.filter { it.isDigit() }
                            }
                        },
                        label = { Text("New Price (Rs.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            cursorColor = BrandBlue
                        )
                    )
                    if (priceError != null) {
                        Text(
                            text = priceError ?: "",
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceInt = newPrice.trim().toIntOrNull()
                        if (priceInt == null || priceInt <= 0) {
                            priceError = "Please enter a valid price."
                            return@Button
                        }
                        viewModel.updateBookingPrice(priceInt) { success ->
                            if (success) {
                                showPriceUpdateDialog = false
                            } else {
                                priceError = "Failed to update price. Please try again."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Update", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriceUpdateDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean,
    booking: Booking?
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

    val isRead = remember(message, booking, isCurrentUser) {
        if (!isCurrentUser) false
        else {
            val otherLastRead = if (message.senderRole == "customer") {
                booking?.providerLastRead
            } else {
                booking?.customerLastRead
            }
            if (otherLastRead != null) {
                message.timestamp.seconds <= otherLastRead.seconds
            } else {
                false
            }
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
                if (message.voiceUrl.isNotEmpty()) {
                    VoicePlayBubble(
                        voiceUrl = message.voiceUrl,
                        duration = message.voiceDuration,
                        isCurrentUser = isCurrentUser
                    )
                } else if (message.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Image message",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = textColor
                    )
                }
                
                if (timeString.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = timeString,
                            fontSize = 9.sp,
                            color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color(0xFF9CA3AF)
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                                contentDescription = if (isRead) "Read" else "Sent",
                                tint = if (isRead) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoicePlayBubble(
    voiceUrl: String,
    duration: Int,
    isCurrentUser: Boolean
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.release()
            } catch (e: Exception) {}
        }
    }
    
    val displayDuration = remember(duration) {
        val mins = duration / 60
        val secs = duration % 60
        String.format(Locale.getDefault(), "%d:%02d", mins, secs)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    try {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        isPlaying = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    try {
                        val player = android.media.MediaPlayer().apply {
                            setDataSource(voiceUrl)
                            prepareAsync()
                            setOnPreparedListener {
                                start()
                            }
                            setOnCompletionListener {
                                isPlaying = false
                                release()
                                mediaPlayer = null
                            }
                            setOnErrorListener { _, _, _ ->
                                isPlaying = false
                                release()
                                mediaPlayer = null
                                true
                            }
                        }
                        mediaPlayer = player
                        isPlaying = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Cannot play audio", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .background(if (isCurrentUser) Color.White.copy(alpha = 0.2f) else BrandBlue.copy(alpha = 0.1f), CircleShape)
                .size(36.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play voice note",
                tint = if (isCurrentUser) Color.White else BrandBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column {
            Text(
                text = "Voice Message",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isCurrentUser) Color.White else Color(0xFF111827)
            )
            Text(
                text = displayDuration,
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color(0xFF6B7280)
            )
        }
    }
}

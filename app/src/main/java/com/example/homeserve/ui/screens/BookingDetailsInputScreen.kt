package com.example.homeserve.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.homeserve.ui.components.PrimaryButton
import com.example.homeserve.ui.theme.BrandBlue
import com.example.homeserve.ui.viewmodel.CustomerViewModel
import java.io.File

@Composable
fun BookingDetailsInputScreen(
    serviceId: String,
    addressId: String,
    viewModel: CustomerViewModel,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Description State
    var descriptionInput by remember { mutableStateOf(viewModel.bookingDescription.value) }
    
    // Synced description with ViewModel
    LaunchedEffect(descriptionInput) {
        viewModel.bookingDescription.value = descriptionInput
    }

    val selectedPhotos = viewModel.bookingPhotoUris.value
    var recordedPath by remember { mutableStateOf<String?>(viewModel.bookingAudioPath.value) }

    // Sync audio path with ViewModel
    LaunchedEffect(recordedPath) {
        viewModel.bookingAudioPath.value = recordedPath
    }

    // Photo Dialog State
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            if (selectedPhotos.size < 3) {
                viewModel.bookingPhotoUris.value = selectedPhotos + uri
            }
        }
    }

    // Camera Capture Launcher
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = tempCameraUri
            if (uri != null) {
                if (selectedPhotos.size < 3) {
                    viewModel.bookingPhotoUris.value = selectedPhotos + uri
                }
            }
        }
    }

    fun launchCamera() {
        try {
            val imagesDir = File(context.cacheDir, "shared_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            val file = File(imagesDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to launch camera", Toast.LENGTH_SHORT).show()
        }
    }

    // Voice recording states
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    var recordingTimer by remember { mutableStateOf(0) }

    // Audio Playback states
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    fun stopRecordingHelper() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        isRecording = false
    }

    fun startRecording() {
        try {
            val file = File.createTempFile("homeserve_voice_", ".m4a", context.cacheDir)
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
            recordedPath = file.absolutePath
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to start audio recording", Toast.LENGTH_SHORT).show()
        }
    }

    // Timer effect for voice note: auto stops at 60s
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTimer = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordingTimer++
                if (recordingTimer >= 60) {
                    stopRecordingHelper()
                    Toast.makeText(context, "Recording limit reached (60 seconds)", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Audio permission request launcher
    val requestAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerAudioRecording() {
        if (isRecording) {
            stopRecordingHelper()
        } else {
            val checkPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            if (checkPermission == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    fun togglePlayback() {
        val path = recordedPath ?: return
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
                    setDataSource(path)
                    prepare()
                    start()
                    setOnCompletionListener {
                        isPlaying = false
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }
                }
                mediaPlayer = player
                isPlaying = true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Playback failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Cleanup resources on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.release()
            mediaPlayer?.release()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Header
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
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Problem Details",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Step 2 of 2",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepBar(true, Modifier.weight(1f))
                StepBar(true, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Describe Problem
                Text(
                    text = "Describe Your Problem",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111827),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Describe what needs to be fixed... (e.g. leaking sink under kitchen pipe, socket sparking when plugged in)", color = Color(0xFF9CA3AF)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section 2: Photo Attachments (up to 3)
                Text(
                    text = "Problem Photos (Up to 3)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111827),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    selectedPhotos.forEach { uri ->
                        Card(
                            modifier = Modifier.size(100.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Problem photo",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.bookingPhotoUris.value = selectedPhotos - uri
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (selectedPhotos.size < 3) {
                        Card(
                            modifier = Modifier.size(100.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .clickable { showImageSourceDialog = true }
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Add Photo",
                                    tint = BrandBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Add Photo",
                                    color = BrandBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Voice Note Record (Up to 60s)
                Text(
                    text = "Explain with Voice Note (Optional)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111827),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isRecording) {
                            Column(
                                modifier = Modifier.clickable { triggerAudioRecording() }.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Recording... ${recordingTimer}s / 60s max", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else if (recordedPath != null) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    IconButton(
                                        onClick = { togglePlayback() },
                                        modifier = Modifier.background(BrandBlue.copy(alpha = 0.1f), CircleShape).size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play voice note",
                                            tint = BrandBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { recordedPath = null },
                                        modifier = Modifier.background(Color(0xFFFEF2F2), CircleShape).size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete voice note", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Voice Explanation Attached", color = Color(0xFF059669), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Column(
                                modifier = Modifier.clickable { triggerAudioRecording() }.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Record Audio explanation", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Bottom Confirmation action
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 16.dp,
            color = Color.White
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                PrimaryButton(
                    text = "Confirm & Place Request",
                    onClick = onConfirmClick,
                    enabled = !isRecording
                )
            }
        }
    }

    // Photo selection dialog choice
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            containerColor = Color.White,
            title = { Text("Select Photo Source", fontWeight = FontWeight.Bold, color = Color(0xFF111827)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showImageSourceDialog = false
                            launchCamera()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take Photo (Camera)", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151))
                    ) {
                        Text("Choose from Gallery", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun StepBar(active: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(4.dp)
            .background(
                color = if (active) BrandBlue else Color(0xFFE5E7EB),
                shape = RoundedCornerShape(2.dp)
            )
    )
}

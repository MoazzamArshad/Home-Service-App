package com.example.homeserve.ui.screens.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.homeserve.ui.viewmodel.AdminViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import com.example.homeserve.data.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProviderDetailDoc(val name: String, val url: String = "")

data class ProviderDetail(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val serviceCategories: List<String>,
    val documents: List<ProviderDetailDoc>,
    val appliedDate: String
)
// Mock details removed in favor of live Firestore provider details

@Composable
fun ProviderDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminViewModel,
    providerId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val providers by viewModel.providersList.collectAsState()
    val provider = providers.find { it.uid == providerId }
    var showRejectDialog by remember { mutableStateOf(false) }
    var previewDocName by remember { mutableStateOf<String?>(null) }

    var profilePhotoBitmap by remember(provider?.profilePhotoUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(provider?.profilePhotoUrl) {
        provider?.profilePhotoUrl?.let { url ->
            if (url.startsWith("content://")) {
                try {
                    val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(url))
                    profilePhotoBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    if (provider == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandBlue)
        }
        return
    }

    val categories = remember(provider.categoryId) {
        provider.categoryId.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    val documents = remember(provider.documentUrl) {
        if (provider.documentUrl.isNotEmpty()) {
            val displayName = when {
                provider.documentUrl.startsWith("content://") -> {
                    val lastSegment = provider.documentUrl.substringAfterLast("/")
                    val decodedSegment = Uri.decode(lastSegment)
                    val cleanName = decodedSegment.substringAfterLast("/")
                    val finalClean = cleanName.substringAfterLast(":")
                    if (finalClean.isNotBlank() && finalClean.contains(".")) finalClean else "id_card_front.jpg"
                }
                provider.documentUrl.startsWith("http://") || provider.documentUrl.startsWith("https://") -> {
                    val lastSegment = provider.documentUrl.substringAfterLast("/")
                    if (lastSegment.isNotBlank() && lastSegment.contains(".")) lastSegment else "provider_document.pdf"
                }
                else -> {
                    "id_card_front.jpg"
                }
            }
            listOf(ProviderDetailDoc(displayName, provider.documentUrl))
        } else {
            listOf(ProviderDetailDoc("id_card_front.jpg", ""))
        }
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Provider") },
            text = { Text("Are you sure you want to reject this provider application?") },
            confirmButton = {
                TextButton(onClick = {
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        showRejectDialog = false
                        viewModel.rejectProvider(provider.uid)
                        onNavigateBack()
                    } else {
                        Toast.makeText(context, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Reject", color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                        text = "Provider Details",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Application Review",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Profile Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(70.dp),
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val photoUrl = provider.profilePhotoUrl
                                if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
                                    coil.compose.AsyncImage(
                                        model = photoUrl,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else if (profilePhotoBitmap != null) {
                                    Image(
                                        bitmap = profilePhotoBitmap!!.asImageBitmap(),
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = if (provider.name.isNotEmpty()) provider.name.first().toString() else "P",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlue
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = provider.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF111827)
                            )
                            val statusText = when {
                                provider.isApproved -> "APPROVED"
                                provider.isRejected -> "REJECTED"
                                else -> "PENDING REVIEW"
                            }
                            val statusColor = when {
                                provider.isApproved -> Color(0xFFD1FAE5)
                                provider.isRejected -> Color(0xFFFEE2E2)
                                else -> Color(0xFFFFFBEB)
                            }
                            val textColor = when {
                                provider.isApproved -> Color(0xFF065F46)
                                provider.isRejected -> Color(0xFF991B1B)
                                else -> Color(0xFFB45309)
                            }
                            Surface(
                                color = statusColor,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    Spacer(modifier = Modifier.height(20.dp))

                    ContactItem(icon = Icons.Default.Email, value = provider.email)
                    Spacer(modifier = Modifier.height(12.dp))
                    ContactItem(icon = Icons.Default.Phone, value = provider.phone)
                    if (provider.idNumber.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ContactItem(icon = Icons.Default.Description, value = "ID Proof: ${provider.idNumber}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Categories
            Text("Service Categories", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    categories.forEach { category ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(category, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF374151))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Documents
            Text("Verification Documents", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(12.dp))
            documents.forEach { doc ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (doc.url.startsWith("http://") || doc.url.startsWith("https://")) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(doc.url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        previewDocName = doc.name
                                    }
                                } else {
                                    previewDocName = doc.name
                                }
                            }
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF9CA3AF))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(doc.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF374151))
                        Text("View", color = BrandBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!provider.isApproved) {
                    Button(
                        onClick = {
                            if (NetworkUtils.isNetworkAvailable(context)) {
                                viewModel.approveProvider(provider.uid)
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("Approve", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showRejectDialog = true },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !provider.isRejected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEF2F2),
                            contentColor = Color(0xFFDC2626),
                            disabledContainerColor = Color(0xFFF3F4F6),
                            disabledContentColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text(if (provider.isRejected) "Rejected" else "Reject", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (NetworkUtils.isNetworkAvailable(context)) {
                                viewModel.rejectProvider(provider.uid)
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Revoke Approval", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (previewDocName != null) {
        AlertDialog(
            onDismissRequest = { previewDocName = null },
            title = { Text("ID Card Preview", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Image File Name:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(previewDocName!!, color = BrandBlue, modifier = Modifier.padding(vertical = 8.dp))
                    Text("Verified Status: Under Admin Review", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🖼️", fontSize = 32.sp)
                                Text("ID Card Image", fontSize = 12.sp, color = Color(0xFF1D4ED8))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewDocName = null }) {
                    Text("Close", color = BrandBlue)
                }
            }
        )
    }
}

@Composable
private fun ContactItem(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4B5563))
    }
}

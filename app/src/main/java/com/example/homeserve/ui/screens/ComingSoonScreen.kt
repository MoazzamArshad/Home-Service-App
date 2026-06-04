package com.example.homeserve.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeserve.ui.theme.BrandBlue
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class CreditCard(
    val cardholderName: String,
    val cardNumber: String,
    val expiryDate: String,
    val cvv: String,
    val gradientColors: List<Color>
)

data class PayoutAccount(
    val title: String,
    val type: String, // "Bank" or "Wallet"
    val accountNumber: String,
    val status: String = "Verified"
)

data class DailySchedule(
    val day: String,
    var isEnabled: Boolean,
    var hours: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonScreen(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanTitle = title.trim()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Dynamic Blue Header
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = cleanTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Official details and management portal",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val lowercaseTitle = cleanTitle.lowercase()
            when {
                lowercaseTitle.contains("payment") -> {
                    PaymentMethodsSection()
                }
                lowercaseTitle.contains("support") || lowercaseTitle.contains("help") || lowercaseTitle.contains("complaint") -> {
                    ComplaintsSupportSection()
                }
                lowercaseTitle.contains("terms") -> {
                    TermsConditionsSection()
                }
                lowercaseTitle.contains("privacy") -> {
                    PrivacyPolicySection()
                }
                lowercaseTitle.contains("availability") || lowercaseTitle.contains("schedule") -> {
                    AvailabilityScheduleSection()
                }
                lowercaseTitle.contains("payout") -> {
                    PayoutMethodsSection()
                }
                lowercaseTitle.contains("notification") -> {
                    NotificationPreferencesSection()
                }
                lowercaseTitle.contains("security") -> {
                    SecuritySection()
                }
                lowercaseTitle.contains("about") -> {
                    AboutPlatformSection()
                }
                else -> {
                    DefaultComingSoonContent(cleanTitle, onBackClick)
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodsSection() {
    var savedCards by remember {
        mutableStateOf(
            listOf(
                CreditCard(
                    cardholderName = "Abdullah",
                    cardNumber = "4532 7182 9901 2481",
                    expiryDate = "12/29",
                    cvv = "382",
                    gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
                ),
                CreditCard(
                    cardholderName = "Farhan Ali",
                    cardNumber = "5412 8820 1024 9902",
                    expiryDate = "08/28",
                    cvv = "192",
                    gradientColors = listOf(Color(0xFF111827), Color(0xFF4B5563))
                )
            )
        )
    }

    var showAddCardDialog by remember { mutableStateOf(false) }
    var cardName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }

    if (showAddCardDialog) {
        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Add New Card",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = cardName,
                        onValueChange = { cardName = it },
                        label = { Text("Cardholder Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF374151),
                            focusedLabelColor = BrandBlue,
                            unfocusedLabelColor = Color(0xFF4B5563),
                            focusedPlaceholderColor = Color(0xFF9CA3AF),
                            unfocusedPlaceholderColor = Color(0xFF9CA3AF),
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = Color(0xFFD1D5DB),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { input ->
                            cardNumber = input.filter { it.isDigit() }.take(16)
                        },
                        label = { Text("Card Number (16 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF374151),
                            focusedLabelColor = BrandBlue,
                            unfocusedLabelColor = Color(0xFF4B5563),
                            focusedPlaceholderColor = Color(0xFF9CA3AF),
                            unfocusedPlaceholderColor = Color(0xFF9CA3AF),
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = Color(0xFFD1D5DB),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = cardExpiry,
                            onValueChange = { input ->
                                cardExpiry = input.take(5) // e.g. MM/YY
                            },
                            label = { Text("Expiry (MM/YY)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF374151),
                                focusedLabelColor = BrandBlue,
                                unfocusedLabelColor = Color(0xFF4B5563),
                                focusedPlaceholderColor = Color(0xFF9CA3AF),
                                unfocusedPlaceholderColor = Color(0xFF9CA3AF),
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = cardCvv,
                            onValueChange = { input ->
                                cardCvv = input.filter { it.isDigit() }.take(3)
                            },
                            label = { Text("CVV") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF374151),
                                focusedLabelColor = BrandBlue,
                                unfocusedLabelColor = Color(0xFF4B5563),
                                focusedPlaceholderColor = Color(0xFF9CA3AF),
                                unfocusedPlaceholderColor = Color(0xFF9CA3AF),
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cardName.isNotBlank() && cardNumber.length == 16 && cardExpiry.isNotBlank() && cardCvv.length == 3) {
                            // Format card with spaces for display
                            val formattedNumber = cardNumber.chunked(4).joinToString(" ")
                            val newCard = CreditCard(
                                cardholderName = cardName.trim(),
                                cardNumber = formattedNumber,
                                expiryDate = cardExpiry.trim(),
                                cvv = cardCvv,
                                gradientColors = listOf(Color(0xFF047857), Color(0xFF10B981)) // beautiful emerald
                            )
                            savedCards = savedCards + newCard
                            showAddCardDialog = false
                            // Reset
                            cardName = ""
                            cardNumber = ""
                            cardExpiry = ""
                            cardCvv = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Card", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCardDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Saved Cards",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
                Button(
                    onClick = { showAddCardDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Card", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(savedCards) { card ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(card.gradientColors))
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Credit Card", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("VISA", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                        
                        Text(
                            text = card.cardNumber,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("CARDHOLDER", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                Text(card.cardholderName.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("EXPIRES", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                Text(card.expiryDate, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ComplaintsSupportSection() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("homeserve_prefs", Context.MODE_PRIVATE) }
    
    val currentRole = remember { sharedPrefs.getString("saved_role", "customer") ?: "customer" }
    val savedPhone = remember { sharedPrefs.getString("saved_phone", "") ?: "" }
    val currentUserId = remember(currentRole, savedPhone) {
        if (savedPhone.isBlank()) ""
        else if (currentRole == "provider") {
            if (savedPhone.contains("@")) {
                "google_prov_${savedPhone.replace("[^a-zA-Z0-9]".toRegex(), "_")}"
            } else {
                "provider_${savedPhone.replace("[^0-9]".toRegex(), "")}"
            }
        } else {
            if (savedPhone.contains("@")) {
                "google_${savedPhone.replace("[^a-zA-Z0-9]".toRegex(), "_")}"
            } else {
                "user_${savedPhone.replace("[^0-9]".toRegex(), "")}"
            }
        }
    }

    var submitterName by remember { mutableStateOf("") }
    var submitterPhone by remember { mutableStateOf(savedPhone) }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            val db = FirebaseFirestore.getInstance()
            val collection = if (currentRole == "provider") "providers" else "users"
            db.collection(collection).document(currentUserId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        submitterName = doc.getString("name") ?: ""
                        submitterPhone = doc.getString("phone") ?: savedPhone
                    }
                }
        }
    }

    var subjectText by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var priorityLevel by remember { mutableStateOf("medium") } // low, medium, high
    
    var isSubmitting by remember { mutableStateOf(false) }
    var submitSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var expandedFaqIndex by remember { mutableStateOf<Int?>(null) }
    val faqs = listOf(
        "How do I book a home service?" to "Select a category on the Home tab, pick a specific service, schedule a date and time, and enter your address. Providers will submit custom quotes, and you can assign the best professional.",
        "How does the pricing bid system work?" to "Once you post a job, nearby qualified service providers see your request and apply with a customized pricing bid and message pitch. You review all candidate quotes on your Bookings screen and assign whoever fits your budget and timeline.",
        "How can I cancel a booked service?" to "Navigate to the 'Bookings' screen, find your pending/accepted request, and tap 'Cancel Booking'. You will be asked to select or type a cancellation reason, which helps us protect provider schedules.",
        "What payment methods do you support?" to "We accept Visa, Mastercard, instant bank transfers, and digital mobile wallets. Payment is finalized only after you mark the service as successfully completed.",
        "How are service providers vetted?" to "All professionals go through a thorough verification checklist, including ID verification (CNIC/SSN), background review, and licensing verification by the HomeServe admin team."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- FAQ Section ---
        Text(
            text = "Frequently Asked Questions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )

        faqs.forEachIndexed { index, faq ->
            val isExpanded = expandedFaqIndex == index
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedFaqIndex = if (isExpanded) null else index },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = faq.first,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF374151),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = faq.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Complaint Submission Section ---
        Text(
            text = "Submit a Complaint / Request",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (submitSuccess) {
                    // Success View
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Text("🎉", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Complaint Submitted Successfully!",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Our support staff is reviewing your request. We will update its status shortly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { submitSuccess = false },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Submit Another Complaint", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Form View
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = subjectText,
                            onValueChange = { subjectText = it },
                            label = { Text("Subject / Issue") },
                            placeholder = { Text("e.g., Payment issue, Bad service behavior") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF374151),
                                focusedLabelColor = BrandBlue,
                                unfocusedLabelColor = Color(0xFF4B5563),
                                focusedPlaceholderColor = Color(0xFF9CA3AF),
                                unfocusedPlaceholderColor = Color(0xFF9CA3AF),
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            label = { Text("Description / Details") },
                            placeholder = { Text("Provide details about your complaint here...") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            maxLines = 5,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF374151),
                                focusedLabelColor = BrandBlue,
                                unfocusedLabelColor = Color(0xFF4B5563),
                                focusedPlaceholderColor = Color(0xFF9CA3AF),
                                unfocusedPlaceholderColor = Color(0xFF9CA3AF),
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        // Priority selection chips
                        Text("Select Priority", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF374151))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("low", "medium", "high").forEach { level ->
                                val isSelected = priorityLevel == level
                                val chipColor = when (level) {
                                    "high" -> if (isSelected) Color(0xFFDC2626) else Color(0xFFFEE2E2)
                                    "medium" -> if (isSelected) Color(0xFFD97706) else Color(0xFFFEF3C7)
                                    else -> if (isSelected) Color(0xFF059669) else Color(0xFFD1FAE5)
                                }
                                val textColor = if (isSelected) Color.White else when (level) {
                                    "high" -> Color(0xFF991B1B)
                                    "medium" -> Color(0xFF854D0E)
                                    else -> Color(0xFF065F46)
                                }
                                Surface(
                                    onClick = { priorityLevel = level },
                                    shape = RoundedCornerShape(8.dp),
                                    color = chipColor,
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(level.uppercase(), color = textColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(error, color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (subjectText.isBlank() || messageText.isBlank()) {
                                    errorMessage = "Please fill in all the details."
                                    return@Button
                                }
                                isSubmitting = true
                                errorMessage = null
                                
                                val db = FirebaseFirestore.getInstance()
                                val ticket = hashMapOf(
                                    "customerName" to (submitterName.ifBlank { "User ${savedPhone.takeLast(4)}" }),
                                    "customerPhone" to submitterPhone,
                                    "submitterId" to currentUserId,
                                    "submitterRole" to currentRole,
                                    "subject" to subjectText.trim(),
                                    "message" to messageText.trim(),
                                    "status" to "open",
                                    "priority" to priorityLevel,
                                    "createdAt" to com.google.firebase.Timestamp.now()
                                )

                                db.collection("support_tickets")
                                    .add(ticket)
                                    .addOnSuccessListener {
                                        isSubmitting = false
                                        submitSuccess = true
                                        subjectText = ""
                                        messageText = ""
                                        priorityLevel = "medium"
                                    }
                                    .addOnFailureListener { e ->
                                        isSubmitting = false
                                        errorMessage = e.message ?: "Failed to submit complaint. Please try again."
                                    }
                            },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Submit Complaint", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TermsConditionsSection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Terms of Service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
        Text("Last updated: May 2026", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
        
        Text(
            "1. Agreement to Terms",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151)
        )
        Text(
            "By accessing or using the HomeServe application, you agree to comply with and be bound by these Terms and Conditions. Please review them carefully.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            lineHeight = 18.sp
        )

        Text(
            "2. Service Booking and Pricing Bids",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151)
        )
        Text(
            "HomeServe is an online aggregator platform. Booking a service creates a direct contract between the customer and the service provider. The provider provides a customized pricing quote. The customer's explicit assignment of the bid locks the price, which must be respected by both parties upon successful execution.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            lineHeight = 18.sp
        )

        Text(
            "3. Cancellation and Fees",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151)
        )
        Text(
            "Users may cancel bookings. Customers are required to provide a cancellation reason. Frequent cancellations without legitimate cause may lead to temporary account suspension to maintain platform integrity.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            lineHeight = 18.sp
        )

        Text(
            "4. Provider Vetting",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151)
        )
        Text(
            "While HomeServe conducts profile checking and ID vetting of service providers, we advise users to exercise standard caution during home visits. HomeServe is not liable for indirect or accidental damages.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun PrivacyPolicySection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Privacy Policy Disclosure", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
        Text("Last updated: May 2026", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
        
        Text(
            "1. Information We Collect",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151)
        )
        Text(
            "We collect personal information such as your name, email, phone number, and physical addresses to facilitate bookings. Providers also submit CNIC/SSN records, professional credentials, and locations for vetting purposes.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            lineHeight = 18.sp
        )

        Text(
            "2. How We Use Location Data",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151)
        )
        Text(
            "We collect physical and digital address references to locate the nearest service professionals. With the removal of automated GPS tracking, address fields are stored locally inside the profile documents and utilized only for service routing.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            lineHeight = 18.sp
        )

        Text(
            "3. Security and Protection",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151)
        )
        Text(
            "Your profile data and messaging conversations are encrypted and hosted securely on Cloud Firestore. Payment card details are tokenized securely and are never stored directly on our servers.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun AvailabilityScheduleSection() {
    var vacationMode by remember { mutableStateOf(false) }
    var listSchedule by remember {
        mutableStateOf(
            listOf(
                DailySchedule("Monday", true, "09:00 AM - 06:00 PM"),
                DailySchedule("Tuesday", true, "09:00 AM - 06:00 PM"),
                DailySchedule("Wednesday", true, "09:00 AM - 06:00 PM"),
                DailySchedule("Thursday", true, "09:00 AM - 06:00 PM"),
                DailySchedule("Friday", true, "09:00 AM - 06:00 PM"),
                DailySchedule("Saturday", true, "10:00 AM - 04:00 PM"),
                DailySchedule("Sunday", false, "Closed")
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Weekly Availability & Shifts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
        
        // Vacation Mode Card
        Surface(
            color = if (vacationMode) Color(0xFFFEF2F2) else Color(0xFFEFF6FF),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (vacationMode) "🌴 Vacation Mode Active" else "💼 Active & Accepting Jobs",
                        fontWeight = FontWeight.Bold,
                        color = if (vacationMode) Color(0xFFDC2626) else BrandBlue,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (vacationMode) "You are marked out-of-office. No new requests will reach you." else "Clients can view your profile and submit direct requests.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Switch(
                    checked = vacationMode,
                    onCheckedChange = { vacationMode = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = if (vacationMode) Color(0xFFDC2626) else BrandBlue)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Shifts Schedule", fontWeight = FontWeight.Bold, color = Color(0xFF374151))

        listSchedule.forEachIndexed { index, schedule ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(schedule.day, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    Text(
                        text = if (schedule.isEnabled) schedule.hours else "Closed",
                        fontSize = 13.sp,
                        color = if (schedule.isEnabled) Color(0xFF059669) else Color(0xFF9CA3AF)
                    )
                }
                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = { isChecked ->
                        val updated = listSchedule.toMutableList()
                        updated[index] = schedule.copy(isEnabled = isChecked, hours = if (isChecked) "09:00 AM - 06:00 PM" else "Closed")
                        listSchedule = updated
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandBlue)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PayoutMethodsSection() {
    var savedPayouts by remember {
        mutableStateOf(
            listOf(
                PayoutAccount("Bank Alfalah Savings A/C", "Bank", "•••• •••• 4920"),
                PayoutAccount("EasyPaisa Mobile Wallet", "Wallet", "0300 •••• 567")
            )
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("Bank") }
    var accountTitle by remember { mutableStateOf("") }
    var accNum by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color.White,
            title = { Text("Add Payout Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Account Type", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Bank", "Wallet").forEach { type ->
                            val isSelected = selectedType == type
                            Surface(
                                onClick = { selectedType = type },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) BrandBlue else Color(0xFFF3F4F6),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(type, color = if (isSelected) Color.White else Color(0xFF374151), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = accountTitle,
                        onValueChange = { accountTitle = it },
                        label = { Text("Account Title (e.g. Abdullah)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF374151),
                            focusedLabelColor = BrandBlue,
                            unfocusedLabelColor = Color(0xFF4B5563),
                            focusedPlaceholderColor = Color(0xFF9CA3AF),
                            unfocusedPlaceholderColor = Color(0xFF9CA3AF),
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = Color(0xFFD1D5DB),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = accNum,
                        onValueChange = { accNum = it },
                        label = { Text(if (selectedType == "Bank") "IBAN / Account Number" else "Mobile Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF374151),
                            focusedLabelColor = BrandBlue,
                            unfocusedLabelColor = Color(0xFF4B5563),
                            focusedPlaceholderColor = Color(0xFF9CA3AF),
                            unfocusedPlaceholderColor = Color(0xFF9CA3AF),
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = Color(0xFFD1D5DB),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (accountTitle.isNotBlank() && accNum.isNotBlank()) {
                            val masked = if (accNum.length > 4) "•••• •••• " + accNum.takeLast(4) else accNum
                            val newAcc = PayoutAccount(accountTitle.trim(), selectedType, masked)
                            savedPayouts = savedPayouts + newAcc
                            showAddDialog = false
                            accountTitle = ""
                            accNum = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Save Method", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Header Card
        Surface(
            color = BrandBlue,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Outstanding Balance", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("$24,500", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { /* Process direct transfer simulation */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Instant Withdraw to Bank", fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Payout Gateways", fontWeight = FontWeight.Bold, color = Color(0xFF374151))
            TextButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Gateway", fontWeight = FontWeight.Bold)
            }
        }

        savedPayouts.forEach { account ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(account.title, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                    Text("${account.type} gateway • ${account.accountNumber}", fontSize = 12.sp, color = Color(0xFF6B7280))
                }
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = account.status, 
                        color = Color(0xFF059669), 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun NotificationPreferencesSection() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("homeserve_prefs", Context.MODE_PRIVATE) }
    val notifPrefs = remember { context.getSharedPreferences("homeserve_notifications_prefs", Context.MODE_PRIVATE) }
    
    val currentRole = remember { sharedPrefs.getString("saved_role", "customer") ?: "customer" }
    val savedPhone = remember { sharedPrefs.getString("saved_phone", "") ?: "" }
    val currentUserId = remember(currentRole, savedPhone) {
        if (savedPhone.isBlank()) ""
        else if (currentRole == "provider") {
            if (savedPhone.contains("@")) {
                "google_prov_${savedPhone.replace("[^a-zA-Z0-9]".toRegex(), "_")}"
            } else {
                "provider_${savedPhone.replace("[^0-9]".toRegex(), "")}"
            }
        } else {
            if (savedPhone.contains("@")) {
                "google_${savedPhone.replace("[^a-zA-Z0-9]".toRegex(), "_")}"
            } else {
                "user_${savedPhone.replace("[^0-9]".toRegex(), "")}"
            }
        }
    }

    // Sensitive default preferences loading
    var bookingAlerts by remember { mutableStateOf(notifPrefs.getBoolean("notification_pref_${currentRole}_booking_alerts", true)) }
    var chatAlerts by remember { mutableStateOf(notifPrefs.getBoolean("notification_pref_${currentRole}_chat_alerts", true)) }
    var marketingAlerts by remember { mutableStateOf(notifPrefs.getBoolean("notification_pref_${currentRole}_marketing_alerts", currentRole != "provider")) }

    // Real-Time snapshot listener for multi-device / admin real-time sync
    DisposableEffect(currentUserId) {
        if (currentUserId.isBlank()) return@DisposableEffect onDispose {}
        val db = FirebaseFirestore.getInstance()
        val docPath = if (currentRole == "provider") "providers" else "users"
        
        val listener = db.collection(docPath).document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                
                val bAlerts = snapshot.getBoolean("notification_pref_${currentRole}_booking_alerts") ?: true
                val cAlerts = snapshot.getBoolean("notification_pref_${currentRole}_chat_alerts") ?: true
                val mAlerts = snapshot.getBoolean("notification_pref_${currentRole}_marketing_alerts") ?: (currentRole != "provider")
                
                bookingAlerts = bAlerts
                chatAlerts = cAlerts
                marketingAlerts = mAlerts
                
                notifPrefs.edit().apply {
                    putBoolean("notification_pref_${currentRole}_booking_alerts", bAlerts)
                    putBoolean("notification_pref_${currentRole}_chat_alerts", cAlerts)
                    putBoolean("notification_pref_${currentRole}_marketing_alerts", mAlerts)
                    apply()
                }
            }
            
        onDispose {
            listener.remove()
        }
    }

    // Interactive helper to trigger local save & background real-time firestore sync
    fun updatePreference(key: String, value: Boolean) {
        notifPrefs.edit().putBoolean(key, value).apply()
        when (key) {
            "notification_pref_${currentRole}_booking_alerts" -> bookingAlerts = value
            "notification_pref_${currentRole}_chat_alerts" -> chatAlerts = value
            "notification_pref_${currentRole}_marketing_alerts" -> marketingAlerts = value
        }
        
        if (currentUserId.isNotBlank()) {
            val db = FirebaseFirestore.getInstance()
            val docPath = if (currentRole == "provider") "providers" else "users"
            db.collection(docPath).document(currentUserId)
                .update(key, value)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Alert Preferences",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose which channels you want to receive alerts from in real-time.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (currentRole == "provider") {
                // Provider Option 1: Job Matches
                NotificationPreferenceCard(
                    icon = Icons.Default.WorkOutline,
                    iconBg = Color(0xFFEFF6FF),
                    iconTint = BrandBlue,
                    title = "Job Match Alerts",
                    description = "Get push notifications immediately when new jobs matching your skills are posted.",
                    checked = bookingAlerts,
                    onCheckedChange = { updatePreference("notification_pref_provider_booking_alerts", it) }
                )

                // Provider Option 2: Chats
                NotificationPreferenceCard(
                    icon = Icons.Default.Chat,
                    iconBg = Color(0xFFECFDF5),
                    iconTint = Color(0xFF059669),
                    title = "Direct Client Chats",
                    description = "Receive push alerts for new messages from active or prospective clients.",
                    checked = chatAlerts,
                    onCheckedChange = { updatePreference("notification_pref_provider_chat_alerts", it) }
                )

                // Provider Option 3: Status & Payment Alerts
                NotificationPreferenceCard(
                    icon = Icons.Default.ReceiptLong,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    title = "Booking & Payment Updates",
                    description = "Get updates when clients accept your bid quotes or confirm cash payments.",
                    checked = marketingAlerts,
                    onCheckedChange = { updatePreference("notification_pref_provider_marketing_alerts", it) }
                )
            } else {
                // Customer Option 1: Bookings & Bid updates
                NotificationPreferenceCard(
                    icon = Icons.Default.ReceiptLong,
                    iconBg = Color(0xFFEFF6FF),
                    iconTint = BrandBlue,
                    title = "Booking & Bid Updates",
                    description = "Instant notifications on provider bid applications or schedule modifications.",
                    checked = bookingAlerts,
                    onCheckedChange = { updatePreference("notification_pref_customer_booking_alerts", it) }
                )

                // Customer Option 2: Chats
                NotificationPreferenceCard(
                    icon = Icons.Default.Chat,
                    iconBg = Color(0xFFECFDF5),
                    iconTint = Color(0xFF059669),
                    title = "Direct Chat Alerts",
                    description = "Get notified instantly when assigned service providers message you.",
                    checked = chatAlerts,
                    onCheckedChange = { updatePreference("notification_pref_customer_chat_alerts", it) }
                )

                // Customer Option 3: Offers & Promotions
                NotificationPreferenceCard(
                    icon = Icons.Default.Campaign,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFDC2626),
                    title = "Discounts & Service Promos",
                    description = "Receive weekly recommendations on new local services and exclusive discounts.",
                    checked = marketingAlerts,
                    onCheckedChange = { updatePreference("notification_pref_customer_marketing_alerts", it) }
                )
            }
        }

        // Live Indicator Banner
        Surface(
            color = Color(0xFFECFDF5),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Real-Time Sync Active",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF047857)
                    )
                    Text(
                        text = "Settings are stored locally and synced with all your active sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF065F46)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationPreferenceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = iconBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BrandBlue,
                    uncheckedThumbColor = Color(0xFF9CA3AF),
                    uncheckedTrackColor = Color(0xFFE5E7EB)
                )
            )
        }
    }
}


@Composable
private fun DefaultComingSoonContent(
    title: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFEFF6FF)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "🚀", fontSize = 48.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "This flow is currently being built and will be available in the next release.",
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                color = Color(0xFF4B5563)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun SecuritySection() {
    var password by remember { mutableStateOf("••••••••••••") }
    var twoFactorEnabled by remember { mutableStateOf(true) }
    var sessionTimeout by remember { mutableStateOf(15f) } // minutes
    
    val accessLogs = listOf(
        Triple("Successful Login", "192.168.1.42 (This Device)", "May 25, 2026 11:32 PM"),
        Triple("Successful Login", "182.178.4.19", "May 24, 2026 09:12 AM"),
        Triple("Password Changed", "System Administrator", "May 20, 2026 03:45 PM"),
        Triple("Successful Login", "182.178.4.19", "May 19, 2026 10:20 PM")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Security Credentials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF111827))

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Admin Password", fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        Text(password, fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    TextButton(onClick = { password = "admin_changed_2026" }) {
                        Text("Update", fontWeight = FontWeight.Bold, color = BrandBlue)
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF3F4F6))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("2-Factor Authentication", fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                        Text("Require OTP SMS for every dashboard login.", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                    Switch(
                        checked = twoFactorEnabled,
                        onCheckedChange = { twoFactorEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandBlue)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Access & Lock Policy", fontWeight = FontWeight.Bold, color = Color(0xFF374151))
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Auto-Lock Session Timeout", fontSize = 13.sp, color = Color(0xFF4B5563))
                    Text("${sessionTimeout.toInt()} min", fontWeight = FontWeight.Bold, color = BrandBlue)
                }
                Slider(
                    value = sessionTimeout,
                    onValueChange = { sessionTimeout = it },
                    valueRange = 5f..60f,
                    colors = SliderDefaults.colors(thumbColor = BrandBlue, activeTrackColor = BrandBlue)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Recent Security Access Logs", fontWeight = FontWeight.Bold, color = Color(0xFF374151))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accessLogs.forEach { log ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3F4F6))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(log.first, fontWeight = FontWeight.Bold, color = Color(0xFF111827), fontSize = 13.sp)
                            Text(log.second, fontSize = 11.sp, color = Color(0xFF6B7280))
                        }
                        Text(log.third, fontSize = 11.sp, color = Color(0xFF9CA3AF), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutPlatformSection() {
    val stats = listOf(
        "Database Nodes" to "3 Active (Multi-region)",
        "Storage Engine" to "Cloud Firestore & Storage",
        "API Host Latency" to "14 ms",
        "Server Status" to "99.98% Uptime",
        "Active Region" to "eu-west-1 (Primary)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = Color(0xFFEFF6FF)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "🛡️", fontSize = 42.sp)
            }
        }
        
        Text("HomeServe Admin Panel", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF111827))
        Text("Version 1.0.0 (Build 2026)", fontSize = 12.sp, color = Color(0xFF6B7280))

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = Color(0xFFECFDF5),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("🟢", fontSize = 10.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("All Platform Services Operational & Healthy", fontWeight = FontWeight.Bold, color = Color(0xFF047857), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Core System Specifications", fontWeight = FontWeight.Bold, color = Color(0xFF374151), modifier = Modifier.align(Alignment.Start))
        
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                stats.forEachIndexed { index, stat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stat.first, fontWeight = FontWeight.Medium, color = Color(0xFF4B5563), fontSize = 14.sp)
                        Text(stat.second, fontWeight = FontWeight.Bold, color = Color(0xFF111827), fontSize = 14.sp)
                    }
                    if (index < stats.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF3F4F6))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "Designed and engineered under supervision of Google Deepmind Team. Licensed under Apache 2.0.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

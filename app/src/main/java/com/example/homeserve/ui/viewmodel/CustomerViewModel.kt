package com.example.homeserve.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeserve.data.FirestoreRepository
import com.example.homeserve.data.model.Booking
import com.example.homeserve.data.model.Category
import com.example.homeserve.data.model.ServiceModel
import com.example.homeserve.data.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CustomerViewModel : ViewModel() {
    private val repository = FirestoreRepository()
    private var bookingsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private val chatListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    private val _isCustomerLoggedIn = MutableStateFlow(false)
    val isCustomerLoggedIn: StateFlow<Boolean> = _isCustomerLoggedIn.asStateFlow()

    var currentUserId = "test_user_id_123"
        private set
    var loggedInPhone = ""
        private set
    var verificationId = ""
        private set

    fun sendOtp(
        phone: String,
        activity: android.app.Activity,
        onCodeSent: () -> Unit,
        onError: (String) -> Unit
    ) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val callbacks = object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                viewModelScope.launch {
                    try {
                        auth.signInWithCredential(credential).await()
                        setCustomerId(phone) {
                            onCodeSent()
                        }
                    } catch (e: Exception) {
                        onError(e.message ?: "Verification failed")
                    }
                }
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                onError(e.message ?: "Verification failed")
            }

            override fun onCodeSent(
                verId: String,
                token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = verId
                onCodeSent()
            }
        }

        val formattedPhone = if (phone.startsWith("+")) phone else "+92${phone.trim().removePrefix("0")}"

        val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (verificationId.isEmpty()) {
            onError("Verification ID is missing. Please request a new code.")
            return
        }
        val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, code)
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(task.exception?.message ?: "Invalid OTP code.")
                }
            }
    }

    private val _userProfile = MutableStateFlow<com.example.homeserve.data.model.User?>(null)
    val userProfile: StateFlow<com.example.homeserve.data.model.User?> = _userProfile.asStateFlow()

    fun signInWithGoogle(email: String, name: String, onCheckResult: (Boolean) -> Unit) {
        loggedInPhone = email
        currentUserId = "google_${email.replace("[^a-zA-Z0-9]".toRegex(), "_")}"
        _isCustomerLoggedIn.value = true
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Register/Sync into Firebase Authentication user list
                try {
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    auth.createUserWithEmailAndPassword(email, "GooglePass_${email.take(5)}")
                        .addOnCompleteListener { t ->
                            if (!t.isSuccessful) {
                                auth.signInWithEmailAndPassword(email, "GooglePass_${email.take(5)}")
                            }
                        }
                } catch (ae: Exception) {
                    ae.printStackTrace()
                }

                val userRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                
                val doc = userRef.get().await()
                var exists = false
                val profile = if (doc.exists()) {
                    val existingUser = doc.toObject(com.example.homeserve.data.model.User::class.java)
                    if (existingUser != null && existingUser.name.isNotBlank()) {
                        exists = true
                    }
                    existingUser
                } else {
                    com.example.homeserve.data.model.User(
                        uid = currentUserId,
                        name = name,
                        email = email,
                        phone = "",
                        role = "user",
                        address = ""
                    )
                }
                _userProfile.value = profile
                
                if (!exists && profile != null) {
                    userRef.set(profile).await()
                }
                
                fetchUserBookings()
                _isLoading.value = false
                onCheckResult(exists)
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                onCheckResult(false)
            }
        }
    }

    fun setCustomerId(phone: String, onCheckResult: (Boolean) -> Unit) {
        loggedInPhone = phone
        currentUserId = "user_${phone.replace("[^0-9]".toRegex(), "")}"
        _isCustomerLoggedIn.value = true
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Check if user document exists in Firestore
                val userRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                
                val doc = userRef.get().await()
                var exists = false
                val profile = if (doc.exists()) {
                    val existingUser = doc.toObject(com.example.homeserve.data.model.User::class.java)
                    if (existingUser != null && existingUser.name.isNotBlank()) {
                        exists = true
                    }
                    existingUser
                } else {
                    null
                }
                _userProfile.value = profile
                
                // Fetch bookings for this specific user
                fetchUserBookings()
                _isLoading.value = false
                onCheckResult(exists)
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                onCheckResult(false)
            }
        }
    }

    fun updateCustomerProfile(
        name: String,
        address: String,
        email: String = "",
        phone: String = "",
        password: String = "",
        context: android.content.Context? = null,
        newPhotoUri: android.net.Uri? = null,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            var success = false
            try {
                val userRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)

                val currentProfile = _userProfile.value
                val resolvedEmail = email.ifBlank {
                    currentProfile?.email ?: "customer_${loggedInPhone.replace("[^0-9]".toRegex(), "")}@example.com"
                }

                // Upload profile photo to Firebase Storage if a new one was picked
                var finalPhotoUrl = currentProfile?.profilePhotoUrl ?: ""
                if (newPhotoUri != null && context != null) {
                    try {
                        val storageRepo = com.example.homeserve.data.StorageRepository()
                        val uploaded = storageRepo.uploadFile(
                            context,
                            newPhotoUri,
                            "customer_photos/$currentUserId"
                        )
                        if (uploaded != null) finalPhotoUrl = uploaded
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val newUser = if (currentProfile != null) {
                    currentProfile.copy(
                        name = name,
                        email = resolvedEmail,
                        phone = if (phone.isNotBlank()) phone else currentProfile.phone,
                        address = address,
                        password = password.ifBlank { currentProfile.password },
                        profilePhotoUrl = finalPhotoUrl
                    )
                } else {
                    com.example.homeserve.data.model.User(
                        uid = currentUserId,
                        name = name,
                        email = resolvedEmail,
                        phone = if (phone.isNotBlank()) phone else loggedInPhone,
                        role = "user",
                        address = address,
                        password = password,
                        profilePhotoUrl = finalPhotoUrl
                    )
                }

                userRef.set(newUser).await()
                _userProfile.value = newUser
                success = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isLoading.value = false
            onResult(success)
        }
    }

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _popularServices = MutableStateFlow<List<ServiceModel>>(emptyList())
    val popularServices: StateFlow<List<ServiceModel>> = _popularServices.asStateFlow()

    private val _categoryServices = MutableStateFlow<List<ServiceModel>>(emptyList())
    val categoryServices: StateFlow<List<ServiceModel>> = _categoryServices.asStateFlow()

    private val _userBookings = MutableStateFlow<List<Booking>>(emptyList())
    val userBookings: StateFlow<List<Booking>> = _userBookings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _availableProviders = MutableStateFlow<List<com.example.homeserve.data.model.Provider>>(emptyList())
    val availableProviders: StateFlow<List<com.example.homeserve.data.model.Provider>> = _availableProviders.asStateFlow()

    private val _allProvidersMap = MutableStateFlow<Map<String, com.example.homeserve.data.model.Provider>>(emptyMap())
    val allProvidersMap: StateFlow<Map<String, com.example.homeserve.data.model.Provider>> = _allProvidersMap.asStateFlow()

    fun fetchAllProviders() {
        viewModelScope.launch {
            val all = repository.getAllProviders()
            _allProvidersMap.value = all.associateBy { it.uid }
        }
    }

    fun fetchAvailableProvidersForCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val all = repository.getAllProviders()
            _availableProviders.value = all.filter {
                it.categoryId.equals(categoryId, ignoreCase = true) && it.isApproved && it.isAvailable
            }
            _isLoading.value = false
        }
    }

    init {
        fetchInitialData()
    }

    private fun fetchInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.seedDatabaseIfNeeded() // Seed the database if empty
            _categories.value = repository.getCategories()
            _popularServices.value = repository.getPopularServices()
            _isLoading.value = false
        }
    }

    fun fetchServicesByCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _categoryServices.value = repository.getServicesByCategory(categoryId)
            _isLoading.value = false
        }
    }

    fun startListeningToUserBookings() {
        if (!_isCustomerLoggedIn.value) return
        bookingsListenerRegistration?.remove()
        
        _isLoading.value = true
        bookingsListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("bookings")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (!_isCustomerLoggedIn.value) return@addSnapshotListener
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Trigger native real-time notifications for booking updates
                    try {
                        val ctx = com.example.homeserve.HomeServeApp.getContext()
                        val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
                        var lastActiveTime = prefs.getLong("last_active_${currentUserId}_bookings", 0L)
                        if (lastActiveTime == 0L) {
                            lastActiveTime = System.currentTimeMillis() - 5000
                            prefs.edit().putLong("last_active_${currentUserId}_bookings", lastActiveTime).apply()
                        }

                        for (change in snapshot.documentChanges) {
                            val booking = change.document.toObject(Booking::class.java).copy(bookingId = change.document.id)
                            val serviceName = booking.serviceName.ifBlank { "Service Request" }
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                if (booking.status == "pending") {
                                    val createdAtMs = booking.createdAt.toDate().time
                                    val notKey = "notified_${currentUserId}_${booking.bookingId}_placed"
                                    if (createdAtMs > lastActiveTime - 10000 && !prefs.getBoolean(notKey, false)) {
                                        prefs.edit().putBoolean(notKey, true).apply()
                                        com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                            context = ctx,
                                            title = "Booking Placed successfully!",
                                            message = "Your request for $serviceName has been submitted.",
                                            targetScreen = "bookings"
                                        )
                                    }
                                } else {
                                    val updatedAtMs = booking.updatedAt.toDate().time
                                    val notKey = "notified_${currentUserId}_${booking.bookingId}_${booking.status}"
                                    if (updatedAtMs > lastActiveTime - 10000 && !prefs.getBoolean(notKey, false)) {
                                        if (booking.status == "cancelled" && booking.providerId.isBlank()) {
                                            // Skip cancellation notification if it was cancelled before being accepted by a provider
                                        } else {
                                            prefs.edit().putBoolean(notKey, true).apply()
                                            val title = when (booking.status) {
                                                "accepted" -> "Booking Accepted 🎉"
                                                "in_progress" -> "Service Started 🛠️"
                                                "completed" -> "Service Completed ✅"
                                                "cancelled" -> "Booking Cancelled ❌"
                                                else -> "Booking Status Updated"
                                            }
                                            val message = when (booking.status) {
                                                "accepted" -> "A provider has accepted your request for $serviceName!"
                                                "in_progress" -> "Your provider is now working on your $serviceName request."
                                                "completed" -> "Your booking for $serviceName has been marked as completed."
                                                "cancelled" -> "Your booking for $serviceName was cancelled."
                                                else -> "Your $serviceName request is now ${booking.status}."
                                            }
                                            com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                context = ctx,
                                                title = title,
                                                message = message,
                                                targetScreen = "bookings"
                                            )
                                        }
                                    }
                                }
                            } else if (change.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                                val oldBooking = _userBookings.value.find { it.bookingId == booking.bookingId }
                                

                                
                                // Notify if status changed
                                if (oldBooking == null || oldBooking.status != booking.status) {
                                    val updatedAtMs = booking.updatedAt.toDate().time
                                    val notKey = "notified_${currentUserId}_${booking.bookingId}_${booking.status}"
                                    if (updatedAtMs > lastActiveTime - 10000 && !prefs.getBoolean(notKey, false)) {
                                        if (booking.status == "cancelled" && booking.providerId.isBlank()) {
                                            // Skip cancellation notification if not accepted by a provider
                                        } else {
                                            prefs.edit().putBoolean(notKey, true).apply()
                                            val title = when (booking.status) {
                                                "accepted" -> "Booking Accepted 🎉"
                                                "in_progress" -> "Service Started 🛠️"
                                                "completed" -> "Service Completed ✅"
                                                "cancelled" -> "Booking Cancelled ❌"
                                                else -> "Booking Status Updated"
                                            }
                                            val message = when (booking.status) {
                                                "accepted" -> "A provider has accepted your request for $serviceName!"
                                                "in_progress" -> "Your provider is now working on your $serviceName request."
                                                "completed" -> "Your booking for $serviceName has been marked as completed."
                                                "cancelled" -> "Your booking for $serviceName was cancelled."
                                                else -> "Your $serviceName request is now ${booking.status}."
                                            }
                                            com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                context = ctx,
                                                title = title,
                                                message = message,
                                                targetScreen = "bookings"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        prefs.edit().putLong("last_active_${currentUserId}_bookings", System.currentTimeMillis()).apply()
                    } catch (ne: Exception) {
                        ne.printStackTrace()
                    }

                    viewModelScope.launch {
                        val bookings = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Booking::class.java)?.copy(bookingId = doc.id)
                        }.sortedByDescending { it.createdAt }
                        
                        val resolvedBookings = bookings.map { booking ->
                            if (booking.providerId.isNotBlank()) {
                                try {
                                    val providerSnapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        .collection("providers").document(booking.providerId).get().await()
                                    if (providerSnapshot.exists()) {
                                        val name = providerSnapshot.getString("name") ?: ""
                                        val phone = providerSnapshot.getString("phone") ?: ""
                                        val photoUrl = providerSnapshot.getString("profilePhotoUrl") ?: ""
                                        booking.copy(providerName = name, providerPhone = phone, providerPhotoUrl = photoUrl)
                                    } else if (booking.providerId == "test_provider_id_123") {
                                        booking.copy(providerName = "Test Provider", providerPhone = "+92 300 1234567")
                                    } else {
                                        booking
                                    }
                                } catch (e: Exception) {
                                    if (booking.providerId == "test_provider_id_123") {
                                        booking.copy(providerName = "Test Provider", providerPhone = "+92 300 1234567")
                                    } else {
                                        booking
                                    }
                                }
                            } else {
                                booking
                            }
                        }
                        _userBookings.value = resolvedBookings
                        syncChatListeners(resolvedBookings)
                    }
                }
            }
    }

    fun fetchUserBookings() {
        if (bookingsListenerRegistration == null) {
            startListeningToUserBookings()
        }
    }

    fun stopListening() {
        bookingsListenerRegistration?.remove()
        bookingsListenerRegistration = null
        _isCustomerLoggedIn.value = false
        _userProfile.value = null
        _userBookings.value = emptyList()
        chatListeners.values.forEach { it.remove() }
        chatListeners.clear()
    }

    fun createBooking(
        serviceId: String,
        serviceName: String,
        categoryId: String,
        providerId: String,
        totalAmount: Int,
        address: String,
        latitude: Double,
        longitude: Double,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            var custName = "Customer"
            var custPhone = loggedInPhone
            try {
                val userDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .get().await()
                if (userDoc.exists()) {
                    custName = userDoc.getString("name") ?: "Customer"
                    custPhone = userDoc.getString("phone") ?: loggedInPhone
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            var provName = ""
            var provPhone = ""
            var status = "pending"
            if (providerId.isNotBlank()) {
                try {
                    val provDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("providers")
                        .document(providerId)
                        .get().await()
                    if (provDoc.exists()) {
                        provName = provDoc.getString("name") ?: ""
                        provPhone = provDoc.getString("phone") ?: ""
                        status = "accepted"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val newBooking = Booking(
                userId = currentUserId,
                providerId = providerId,
                serviceId = serviceId,
                serviceName = serviceName,
                categoryId = categoryId,
                totalAmount = totalAmount,
                address = address,
                customerName = custName,
                customerPhone = custPhone,
                customerLatitude = latitude,
                customerLongitude = longitude,
                providerName = provName,
                providerPhone = provPhone,
                status = status
            )
            val success = repository.createBooking(newBooking)
            // Refresh bookings after creation
            fetchUserBookings()
            _isLoading.value = false
            onResult(success)
        }
    }

    fun completeBooking(bookingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateBookingStatus(bookingId, "completed")
            fetchUserBookings()
            _isLoading.value = false
        }
    }

    fun cancelBooking(bookingId: String, reason: String) {
        try {
            val ctx = com.example.homeserve.HomeServeApp.getContext()
            val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("notified_${currentUserId}_${bookingId}_cancelled", true).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateBookingStatus(bookingId, "cancelled", cancelReason = reason)
            fetchUserBookings()
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        bookingsListenerRegistration?.remove()
        chatListeners.values.forEach { it.remove() }
        chatListeners.clear()
    }

    private fun syncChatListeners(bookings: List<Booking>) {
        val activeBookingIds = bookings
            .filter { it.status == "accepted" || it.status == "in_progress" }
            .map { it.bookingId }
            .toSet()

        val iterator = chatListeners.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in activeBookingIds) {
                entry.value.remove()
                iterator.remove()
            }
        }

        val ctx = com.example.homeserve.HomeServeApp.getContext()
        val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)

        for (bookingId in activeBookingIds) {
            if (bookingId !in chatListeners) {
                val lastMsgKey = "last_msg_${currentUserId}_${bookingId}"
                if (prefs.getLong(lastMsgKey, 0L) == 0L) {
                    prefs.edit().putLong(lastMsgKey, System.currentTimeMillis()).apply()
                }

                val registration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("bookings")
                    .document(bookingId)
                    .collection("messages")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                    .addSnapshotListener { msgSnapshot, msgError ->
                        if (msgError != null || msgSnapshot == null) return@addSnapshotListener
                        if (!_isCustomerLoggedIn.value) return@addSnapshotListener
                        
                        val lastMsgTime = prefs.getLong("last_msg_${currentUserId}_${bookingId}", 0L)
                        var newLastTime = lastMsgTime

                        for (change in msgSnapshot.documentChanges) {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                val message = change.document.toObject(ChatMessage::class.java).copy(messageId = change.document.id)
                                val msgTime = message.timestamp.toDate().time
                                if (msgTime > lastMsgTime && message.senderId != currentUserId) {
                                    if (bookingId != ChatViewModel.activeChatBookingId) {
                                        val notKey = "notified_msg_${currentUserId}_${message.messageId}"
                                        if (!prefs.getBoolean(notKey, false)) {
                                            prefs.edit().putBoolean(notKey, true).apply()
                                            com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                context = ctx,
                                                title = "New Message from ${message.senderName} 💬",
                                                message = message.text,
                                                targetScreen = "chat/$bookingId/customer"
                                            )
                                        }
                                    }
                                }
                                if (msgTime > newLastTime) {
                                    newLastTime = msgTime
                                }
                            }
                        }
                        if (newLastTime > lastMsgTime) {
                            prefs.edit().putLong("last_msg_${currentUserId}_${bookingId}", newLastTime).apply()
                        }
                    }
                chatListeners[bookingId] = registration
            }
        }
    }
}

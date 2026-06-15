package com.example.homeserve.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeserve.data.FirestoreRepository
import com.example.homeserve.data.model.Booking
import com.example.homeserve.data.model.Category
import com.example.homeserve.data.model.SavedAddress
import com.example.homeserve.data.model.ServiceModel
import com.example.homeserve.data.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.mutableStateOf

class CustomerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirestoreRepository()
    private val dbHelper = com.example.homeserve.data.LocalCacheDbHelper(application)
    private var bookingsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var userListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var globalBookingsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private val chatListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    private val _isCustomerLoggedIn = MutableStateFlow(false)
    val isCustomerLoggedIn: StateFlow<Boolean> = _isCustomerLoggedIn.asStateFlow()

    private val _isBlockedEvent = MutableStateFlow(false)
    val isBlockedEvent: StateFlow<Boolean> = _isBlockedEvent.asStateFlow()

    private val _savedAddresses = MutableStateFlow<List<SavedAddress>>(emptyList())
    val savedAddresses: StateFlow<List<SavedAddress>> = _savedAddresses.asStateFlow()

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
        // Mock OTP sending: immediately call success callback
        verificationId = "mock_verification_id"
        onCodeSent()
    }

    fun verifyOtp(
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Mock OTP verification: accept any code matching "123456"
        if (code == "123456") {
            onSuccess()
        } else {
            onError("Invalid OTP code. Please enter 123456.")
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
                    if (existingUser != null && existingUser.name.isNotBlank() && existingUser.phone.isNotBlank() && existingUser.address.isNotBlank()) {
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
                if (profile?.isBlocked == true) {
                    _isLoading.value = false
                    _isBlockedEvent.value = true
                    _isCustomerLoggedIn.value = false
                    onCheckResult(false)
                    return@launch
                }
                _userProfile.value = profile
                
                if (!exists && profile != null) {
                    userRef.set(profile).await()
                }
                
                fetchUserBookings()
                startListeningToUserProfile()
                startListeningToNotifications()
                loadSavedAddresses()
                saveFcmTokenForCurrentUser()
                _isLoading.value = false
                onCheckResult(exists)
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                onCheckResult(false)
            }
        }
    }

    // ── Email / Password Authentication ──────────────────────────────────────

    fun signInWithEmail(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: email.replace("[^a-zA-Z0-9]".toRegex(), "_")
                    loggedInPhone = email
                    currentUserId = "email_${uid}"
                    _isCustomerLoggedIn.value = true
                    viewModelScope.launch {
                        _isLoading.value = true
                        try {
                            val userRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users").document(currentUserId)
                            val doc = userRef.get().await()
                            val profile = if (doc.exists()) {
                                doc.toObject(com.example.homeserve.data.model.User::class.java)
                            } else {
                                val newUser = com.example.homeserve.data.model.User(
                                    uid = currentUserId, name = email.substringBefore("@"),
                                    email = email, phone = "", role = "user", address = ""
                                )
                                userRef.set(newUser).await()
                                newUser
                            }
                            if (profile?.isBlocked == true) {
                                _isCustomerLoggedIn.value = false
                                _isBlockedEvent.value = true
                                _isLoading.value = false
                                onResult(false, "Account is blocked. Contact support.")
                                return@launch
                            }
                            _userProfile.value = profile
                            fetchUserBookings()
                            startListeningToUserProfile()
                            startListeningToNotifications()
                            loadSavedAddresses()
                            saveFcmTokenForCurrentUser()
                            _isLoading.value = false
                            onResult(true, null)
                        } catch (e: Exception) {
                            _isLoading.value = false
                            onResult(false, e.message)
                        }
                    }
                } else {
                    val ex = task.exception
                    val msg = when (ex) {
                        is com.google.firebase.auth.FirebaseAuthInvalidUserException -> {
                            "No account found with this email. Please register first."
                        }
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                            "Incorrect password. Please try again."
                        }
                        else -> ex?.message ?: "Sign in failed."
                    }
                    onResult(false, msg)
                }
            }
    }

    fun registerWithEmail(
        name: String,
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: email.replace("[^a-zA-Z0-9]".toRegex(), "_")
                    loggedInPhone = email
                    currentUserId = "email_${uid}"
                    _isCustomerLoggedIn.value = true
                    viewModelScope.launch {
                        _isLoading.value = true
                        try {
                            val newUser = com.example.homeserve.data.model.User(
                                uid = currentUserId, name = name, email = email,
                                phone = "", role = "user", address = ""
                            )
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users").document(currentUserId).set(newUser).await()
                            _userProfile.value = newUser
                            fetchUserBookings()
                            startListeningToUserProfile()
                            startListeningToNotifications()
                            loadSavedAddresses()
                            saveFcmTokenForCurrentUser()
                            _isLoading.value = false
                            onResult(true, null)
                        } catch (e: Exception) {
                            _isLoading.value = false
                            onResult(false, e.message)
                        }
                    }
                } else {
                    val msg = task.exception?.message ?: "Registration failed."
                    onResult(false, msg)
                }
            }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean) -> Unit) {
        com.google.firebase.auth.FirebaseAuth.getInstance()
            .sendPasswordResetEmail(email)
            .addOnCompleteListener { task -> onResult(task.isSuccessful) }
    }

    // ─────────────────────────────────────────────────────────────────────────

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
                if (profile?.isBlocked == true) {
                    _isLoading.value = false
                    _isBlockedEvent.value = true
                    _isCustomerLoggedIn.value = false
                    onCheckResult(false)
                    return@launch
                }
                _userProfile.value = profile
                
                fetchUserBookings()
                startListeningToUserProfile()
                startListeningToNotifications()
                loadSavedAddresses()
                saveFcmTokenForCurrentUser()
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
        email: String = "",
        phone: String = "",
        password: String = "",
        address: String = "",
        bio: String = "",
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
                val resolvedEmail = email.trim()

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
                        address = if (address.isNotBlank()) address else currentProfile.address,
                        bio = bio,
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
                        bio = bio,
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

    private val _allServices = MutableStateFlow<List<ServiceModel>>(emptyList())
    val allServices: StateFlow<List<ServiceModel>> = _allServices.asStateFlow()

    private val _categoryServices = MutableStateFlow<List<ServiceModel>>(emptyList())
    val categoryServices: StateFlow<List<ServiceModel>> = _categoryServices.asStateFlow()

    private val _userBookings = MutableStateFlow<List<Booking>>(emptyList())
    val userBookings: StateFlow<List<Booking>> = _userBookings.asStateFlow()

    private val _hasUnreadBookings = MutableStateFlow(false)
    val hasUnreadBookings: StateFlow<Boolean> = _hasUnreadBookings.asStateFlow()

    private val _notifications = MutableStateFlow<List<com.example.homeserve.data.model.AppNotification>>(emptyList())
    val notifications: StateFlow<List<com.example.homeserve.data.model.AppNotification>> = _notifications.asStateFlow()
    private var notificationsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    private val _lastMessages = MutableStateFlow<Map<String, ChatMessage>>(emptyMap())
    val lastMessages: StateFlow<Map<String, ChatMessage>> = _lastMessages.asStateFlow()

    // ── Temporary Booking States ────────────────────────────────────────────────
    val bookingDescription = mutableStateOf("")
    val bookingContactPhone = mutableStateOf("")
    val bookingPhotoUris = mutableStateOf<List<android.net.Uri>>(emptyList())
    val bookingAudioPath = mutableStateOf<String?>(null)

    fun clearBookingInputs() {
        bookingDescription.value = ""
        bookingContactPhone.value = ""
        bookingPhotoUris.value = emptyList()
        bookingAudioPath.value = null
    }

    private val _chatThreads = MutableStateFlow<List<com.example.homeserve.data.model.ChatThread>>(emptyList())
    val chatThreads: StateFlow<List<com.example.homeserve.data.model.ChatThread>> = _chatThreads.asStateFlow()

    fun startListeningToNotifications() {
        if (currentUserId.isEmpty()) return
        notificationsListenerRegistration?.remove()
        notificationsListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("recipientId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.homeserve.data.model.AppNotification::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    _notifications.value = list
                }
            }
    }

    fun insertNotification(title: String, message: String, targetScreen: String = "bookings") {
        viewModelScope.launch {
            val notif = com.example.homeserve.data.model.AppNotification(
                recipientId = currentUserId,
                title = title,
                message = message,
                targetScreen = targetScreen,
                read = false,
                timestamp = com.google.firebase.Timestamp.now()
            )
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("notifications")
                .add(notif)
        }
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .document(id)
                    .update("read", true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markAllNotificationsAsRead() {
        val currentList = _notifications.value
        val unreadIds = currentList.filter { !it.read }.map { it.id }
        if (unreadIds.isEmpty()) return

        // Optimistically update local flow first
        _notifications.value = currentList.map { it.copy(read = true) }

        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val batch = db.batch()
                for (id in unreadIds) {
                    val ref = db.collection("notifications").document(id)
                    batch.update(ref, "read", true)
                }
                batch.commit().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markChatAsRead(bookingId: String) {
        val ctx = com.example.homeserve.HomeServeApp.getContext()
        val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("unread_chat_${currentUserId}_${bookingId}", false).apply()
        updateUnreadCount()
        rebuildChatThreads()
    }

    fun clearBookingsUpdateBadge() {
        val ctx = com.example.homeserve.HomeServeApp.getContext()
        val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("unread_status_update_${currentUserId}", false).apply()
        updateUnreadCount()
    }

    fun updateUnreadCount() {
        val ctx = com.example.homeserve.HomeServeApp.getContext()
        val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
        val activeBookingIds = _userBookings.value
            .filter { it.status == "accepted" || it.status == "in_progress" }
            .map { it.bookingId }
        
        var hasUnread = prefs.getBoolean("unread_status_update_${currentUserId}", false)
        if (!hasUnread) {
            for (id in activeBookingIds) {
                if (prefs.getBoolean("unread_chat_${currentUserId}_${id}", false)) {
                    hasUnread = true
                    break
                }
            }
        }
        _hasUnreadBookings.value = hasUnread
    }

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
            
            // 1. Load from local SQLite database cache immediately for offline accessibility
            try {
                val cachedCats = dbHelper.getCategories()
                val cachedSrvs = dbHelper.getAllServices()
                if (cachedCats.isNotEmpty()) {
                    _categories.value = cachedCats
                }
                if (cachedSrvs.isNotEmpty()) {
                    _allServices.value = cachedSrvs
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Fetch from Firebase Firestore and update local cache
            try {
                repository.seedDatabaseIfNeeded() // Seed the database if empty
                val remoteCats = repository.getCategories()
                if (remoteCats.isNotEmpty()) {
                    _categories.value = remoteCats
                    dbHelper.saveCategories(remoteCats)
                }
                
                val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("services")
                    .get()
                    .await()
                val remoteSrvs = snapshot.toObjects(ServiceModel::class.java)
                if (remoteSrvs.isNotEmpty()) {
                    _allServices.value = remoteSrvs
                    dbHelper.saveServices(remoteSrvs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            startListeningToPopularServices()
            _isLoading.value = false
        }
    }

    fun startListeningToPopularServices() {
        globalBookingsListenerRegistration?.remove()
        globalBookingsListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("bookings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val bookingsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Booking::class.java)
                    }
                    calculatePopularServices(bookingsList)
                }
            }
    }

    private fun calculatePopularServices(bookings: List<Booking>) {
        val serviceCounts = bookings.groupBy { it.serviceId }
            .mapValues { it.value.size }
        val sortedServiceIds = serviceCounts.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(3)

        val servicesList = _allServices.value
        val topServices = if (servicesList.isNotEmpty()) {
            sortedServiceIds.mapNotNull { id ->
                servicesList.find { it.serviceId == id }
            }
        } else {
            emptyList()
        }

        val finalServices = if (topServices.size < 3 && servicesList.isNotEmpty()) {
            val fallback = servicesList.take(3)
            (topServices + fallback).distinct().take(3)
        } else {
            topServices
        }
        _popularServices.value = finalServices
    }

    fun fetchServicesByCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Try loading from local database cache first
            try {
                val cachedSrvs = dbHelper.getServicesByCategory(categoryId)
                if (cachedSrvs.isNotEmpty()) {
                    _categoryServices.value = cachedSrvs
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Retrieve fresh data from remote database
            try {
                val remoteSrvs = repository.getServicesByCategory(categoryId)
                if (remoteSrvs.isNotEmpty()) {
                    _categoryServices.value = remoteSrvs
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
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
                                        insertNotification("Booking Placed successfully!", "Your request for $serviceName has been submitted.", "bookings")
                                    }
                                } else {
                                    val updatedAtMs = booking.updatedAt.toDate().time
                                    val notKey = "notified_${currentUserId}_${booking.bookingId}_${booking.status}"
                                    if (updatedAtMs > lastActiveTime - 10000 && !prefs.getBoolean(notKey, false)) {
                                        if (booking.status == "cancelled" && booking.providerId.isBlank()) {
                                            // Skip cancellation notification if it was cancelled before being accepted by a provider
                                        } else {
                                            prefs.edit().putBoolean(notKey, true).apply()
                                            prefs.edit().putBoolean("unread_status_update_${currentUserId}", true).apply()
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
                                            insertNotification(title, message, "bookings")
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
                                            prefs.edit().putBoolean("unread_status_update_${currentUserId}", true).apply()
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
                                            insertNotification(title, message, "bookings")
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
                        updateUnreadCount()
                        rebuildChatThreads()
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
        userListenerRegistration?.remove()
        userListenerRegistration = null
        globalBookingsListenerRegistration?.remove()
        globalBookingsListenerRegistration = null
        notificationsListenerRegistration?.remove()
        notificationsListenerRegistration = null
        _isCustomerLoggedIn.value = false
        _userProfile.value = null
        _userBookings.value = emptyList()
        _notifications.value = emptyList()
        _hasUnreadBookings.value = false
        chatListeners.values.forEach { it.remove() }
        chatListeners.clear()
        _chatThreads.value = emptyList()
        _lastMessages.value = emptyMap()
    }

    fun clearBlockedEvent() {
        _isBlockedEvent.value = false
    }

    fun startListeningToUserProfile() {
        if (currentUserId.isEmpty()) return
        userListenerRegistration?.remove()
        userListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(com.example.homeserve.data.model.User::class.java)?.copy(uid = snapshot.id)
                    _userProfile.value = profile
                    if (profile?.isBlocked == true) {
                        triggerBlockedSignOut()
                    }

                    // Sync notification settings in real-time
                    try {
                        val ctx = com.example.homeserve.HomeServeApp.getContext()
                        val notifPrefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
                        
                        val bAlerts = snapshot.getBoolean("notification_pref_customer_booking_alerts") ?: true
                        val cAlerts = snapshot.getBoolean("notification_pref_customer_chat_alerts") ?: true
                        val mAlerts = snapshot.getBoolean("notification_pref_customer_marketing_alerts") ?: false
                        
                        notifPrefs.edit().apply {
                            putBoolean("notification_pref_customer_booking_alerts", bAlerts)
                            putBoolean("notification_pref_customer_chat_alerts", cAlerts)
                            putBoolean("notification_pref_customer_marketing_alerts", mAlerts)
                            apply()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
    }

    private fun triggerBlockedSignOut() {
        _isBlockedEvent.value = true
        viewModelScope.launch {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val ctx = com.example.homeserve.HomeServeApp.getContext()
            val sharedPrefs = ctx.getSharedPreferences("homeserve_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().clear().apply()
            
            val notifPrefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
            notifPrefs.edit().clear().apply()
            
            stopListening()
        }
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
        jobDescription: String = "",
        problemPhotoUrl: String = "",
        problemAudioUrl: String = "",
        customerPhoneInput: String = "",
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val activeCount = _userBookings.value.count { it.status.lowercase() in listOf("pending", "accepted", "in_progress") }
            if (activeCount >= 3) {
                _isLoading.value = false
                onResult(false, "You can book up to 3 services at a time. Please complete or cancel your active bookings first.")
                return@launch
            }
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
                customerPhone = if (customerPhoneInput.isNotBlank()) customerPhoneInput else custPhone,
                customerLatitude = latitude,
                customerLongitude = longitude,
                providerName = provName,
                providerPhone = provPhone,
                status = status,
                jobDescription = jobDescription,
                problemPhotoUrl = problemPhotoUrl,
                problemAudioUrl = problemAudioUrl
            )
            val success = repository.createBooking(newBooking)
            // Refresh bookings after creation
            fetchUserBookings()
            _isLoading.value = false
            onResult(success, if (success) null else "Failed to confirm booking on the server. Please try again.")
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
        notificationsListenerRegistration?.remove()
        globalBookingsListenerRegistration?.remove()
        chatListeners.values.forEach { it.remove() }
        chatListeners.clear()
    }

    fun rebuildChatThreads() {
        val bookingsList = _userBookings.value
        val lastMsgs = _lastMessages.value
        val ctx = com.example.homeserve.HomeServeApp.getContext()
        val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)

        val chatBookings = bookingsList.filter { it.providerId.isNotBlank() && currentUserId !in it.hiddenByUsers }

        val threads = chatBookings.map { booking ->
            val lastMsg = lastMsgs[booking.bookingId]
            val hasUnread = prefs.getBoolean("unread_chat_${currentUserId}_${booking.bookingId}", false)
            
            com.example.homeserve.data.model.ChatThread(
                bookingId = booking.bookingId,
                otherPartyId = booking.providerId,
                otherPartyName = booking.providerName.ifBlank { "Provider" },
                otherPartyPhotoUrl = booking.providerPhotoUrl,
                lastMessageText = lastMsg?.text ?: "Tap to start conversation",
                lastMessageTimestamp = lastMsg?.timestamp ?: booking.updatedAt,
                unreadCount = if (hasUnread) 1 else 0
            )
        }.sortedByDescending { it.lastMessageTimestamp }
        
        _chatThreads.value = threads
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

        // For completed or cancelled bookings, do a one-time fetch of the last message if not already cached
        val nonActiveBookings = bookings.filter { it.providerId.isNotBlank() && it.bookingId !in activeBookingIds }
        nonActiveBookings.forEach { booking ->
            if (booking.bookingId !in _lastMessages.value) {
                viewModelScope.launch {
                    try {
                        val msgSnap = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("bookings")
                            .document(booking.bookingId)
                            .collection("messages")
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .await()
                        val lastMsg = msgSnap.documents.firstOrNull()?.toObject(ChatMessage::class.java)
                        if (lastMsg != null) {
                            _lastMessages.value = _lastMessages.value + (booking.bookingId to lastMsg)
                            rebuildChatThreads()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

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

                        val messagesList = msgSnapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                        val latestMsg = messagesList.lastOrNull()
                        if (latestMsg != null) {
                            _lastMessages.value = _lastMessages.value + (bookingId to latestMsg)
                            rebuildChatThreads()
                        }

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
                                            insertNotification("New Message from ${message.senderName} 💬", message.text, "chat/$bookingId/customer")
                                        }
                                        prefs.edit().putBoolean("unread_chat_${currentUserId}_${bookingId}", true).apply()
                                        updateUnreadCount()
                                        rebuildChatThreads()
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

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications(currentUserId)
        }
    }

    fun deleteChatThread(bookingId: String) {
        viewModelScope.launch {
            repository.hideChatThread(bookingId, currentUserId)
            rebuildChatThreads()
        }
    }

    // ── Saved Addresses ───────────────────────────────────────────────────────

    fun loadSavedAddresses() {
        viewModelScope.launch {
            _savedAddresses.value = repository.getUserAddresses(currentUserId)
        }
    }

    fun addAddress(address: SavedAddress) {
        viewModelScope.launch {
            repository.addUserAddress(currentUserId, address)
            loadSavedAddresses()
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            repository.deleteUserAddress(currentUserId, addressId)
            loadSavedAddresses()
        }
    }

    fun getAddressById(id: String): SavedAddress? {
        return _savedAddresses.value.find { it.id == id }
    }

    // ── FCM Token ─────────────────────────────────────────────────────────────

    private fun saveFcmTokenForCurrentUser() {
        viewModelScope.launch {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        viewModelScope.launch {
                            repository.saveFcmToken(currentUserId, token, "user")
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteAccount(onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.deleteCustomerHistoryAndProfile(currentUserId)
            if (success) {
                val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (authUser != null) {
                    authUser.delete().addOnCompleteListener { task ->
                        _isLoading.value = false
                        if (task.isSuccessful) {
                            stopListening()
                            onComplete(true, null)
                        } else {
                            val errorMsg = task.exception?.message ?: "Authentication deletion failed"
                            onComplete(false, errorMsg)
                        }
                    }
                } else {
                    _isLoading.value = false
                    stopListening()
                    onComplete(true, null)
                }
            } else {
                _isLoading.value = false
                onComplete(false, "Failed to delete database history and profile.")
            }
        }
    }

    fun submitBookingReview(
        bookingId: String,
        rating: Int,
        reviewText: String,
        reviewTags: List<String>,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.submitBookingReview(bookingId, rating, reviewText, reviewTags)
            _isLoading.value = false
            onResult(success)
        }
    }

    private val _serviceReviews = MutableStateFlow<List<Booking>>(emptyList())
    val serviceReviews: StateFlow<List<Booking>> = _serviceReviews.asStateFlow()

    fun fetchReviewsForService(serviceId: String) {
        viewModelScope.launch {
            try {
                val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("bookings")
                    .whereEqualTo("serviceId", serviceId)
                    .get().await()
                
                val reviews = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)
                }.filter { it.rating > 0 }
                
                _serviceReviews.value = reviews
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun signInWithPhoneAndPin(
        phone: String,
        pin: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        loggedInPhone = phone
        currentUserId = "user_${phone.replace("[^0-9]".toRegex(), "")}"
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                val doc = userRef.get().await()
                if (!doc.exists()) {
                    _isLoading.value = false
                    onComplete(false, "Account not found. Please register first.")
                    return@launch
                }
                val profile = doc.toObject(com.example.homeserve.data.model.User::class.java)
                if (profile == null) {
                    _isLoading.value = false
                    onComplete(false, "Failed to load account profile.")
                    return@launch
                }
                
                if (profile.password != pin) {
                    _isLoading.value = false
                    onComplete(false, "Incorrect PIN code. Please try again.")
                    return@launch
                }
                
                if (profile.isBlocked) {
                    _isLoading.value = false
                    _isBlockedEvent.value = true
                    onComplete(false, "Your account has been blocked. Please contact support.")
                    return@launch
                }
                
                _isCustomerLoggedIn.value = true
                _userProfile.value = profile
                fetchUserBookings()
                startListeningToUserProfile()
                startListeningToNotifications()
                loadSavedAddresses()
                saveFcmTokenForCurrentUser()
                _isLoading.value = false
                onComplete(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                onComplete(false, e.message ?: "Authentication failed.")
            }
        }
    }

    fun registerWithPhoneAndPin(
        name: String,
        phone: String,
        email: String,
        pin: String,
        address: String,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        onComplete: (Boolean, String?) -> Unit
    ) {
        loggedInPhone = phone
        currentUserId = "user_${phone.replace("[^0-9]".toRegex(), "")}"
        viewModelScope.launch {
            _isLoading.value = true
            try {
                var resolvedLat = latitude
                var resolvedLon = longitude
                if (resolvedLat == 0.0 && resolvedLon == 0.0) {
                    resolvedLat = 31.5204
                    resolvedLon = 74.3587
                    try {
                        val ctx = com.example.homeserve.HomeServeApp.getContext()
                        val geocoder = android.location.Geocoder(ctx)
                        val list = geocoder.getFromLocationName(address, 1)
                        if (!list.isNullOrEmpty()) {
                            resolvedLat = list[0].latitude
                            resolvedLon = list[0].longitude
                        } else {
                            val addrLower = address.lowercase()
                            resolvedLat = when {
                                addrLower.contains("model town") -> 31.4790
                                addrLower.contains("gulberg") -> 31.5222
                                addrLower.contains("dha") -> 31.4697
                                addrLower.contains("johar town") -> 31.4697
                                addrLower.contains("kalma chowk") || addrLower.contains("kalma chawk") -> 31.5065
                                else -> 31.5204
                            }
                            resolvedLon = when {
                                addrLower.contains("model town") -> 74.3216
                                addrLower.contains("gulberg") -> 74.3587
                                addrLower.contains("dha") -> 74.4072
                                addrLower.contains("johar town") -> 74.2728
                                addrLower.contains("kalma chowk") || addrLower.contains("kalma chawk") -> 74.3321
                                else -> 74.3587
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val addrLower = address.lowercase()
                        resolvedLat = when {
                            addrLower.contains("model town") -> 31.4790
                            addrLower.contains("gulberg") -> 31.5222
                            addrLower.contains("dha") -> 31.4697
                            addrLower.contains("johar town") -> 31.4697
                            addrLower.contains("kalma chowk") || addrLower.contains("kalma chawk") -> 31.5065
                            else -> 31.5204
                        }
                        resolvedLon = when {
                            addrLower.contains("model town") -> 74.3216
                            addrLower.contains("gulberg") -> 74.3587
                            addrLower.contains("dha") -> 74.4072
                            addrLower.contains("johar town") -> 74.2728
                            addrLower.contains("kalma chowk") || addrLower.contains("kalma chawk") -> 74.3321
                            else -> 74.3587
                        }
                    }
                }

                val userRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                val doc = userRef.get().await()
                if (doc.exists()) {
                    _isLoading.value = false
                    onComplete(false, "An account with this phone number already exists.")
                    return@launch
                }
                
                val newUser = com.example.homeserve.data.model.User(
                    uid = currentUserId,
                    name = name,
                    email = email,
                    phone = phone,
                    role = "user",
                    address = address,
                    password = pin,
                    createdAt = com.google.firebase.Timestamp.now()
                )
                
                userRef.set(newUser).await()
                
                try {
                    val savedAddr = com.example.homeserve.data.model.SavedAddress(
                        id = "default_address",
                        label = "Default Home",
                        address = address,
                        latitude = resolvedLat,
                        longitude = resolvedLon,
                        isDefault = true
                    )
                    userRef.collection("addresses").document("default_address").set(savedAddr).await()
                } catch (addrEx: Exception) {
                    addrEx.printStackTrace()
                }
                
                _isCustomerLoggedIn.value = true
                _userProfile.value = newUser
                fetchUserBookings()
                startListeningToUserProfile()
                startListeningToNotifications()
                loadSavedAddresses()
                saveFcmTokenForCurrentUser()
                _isLoading.value = false
                onComplete(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                onComplete(false, e.message ?: "Registration failed.")
            }
        }
    }
}


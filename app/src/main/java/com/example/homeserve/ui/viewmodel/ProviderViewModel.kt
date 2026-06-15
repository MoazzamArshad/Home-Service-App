package com.example.homeserve.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeserve.data.FirestoreRepository
import com.example.homeserve.data.LocationUtils
import com.example.homeserve.data.model.Booking
import com.example.homeserve.data.model.Provider
import com.example.homeserve.data.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProviderViewModel : ViewModel() {
    private val repository = FirestoreRepository()
    private val chatListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    private val _isProviderLoggedIn = MutableStateFlow(false)
    val isProviderLoggedIn: StateFlow<Boolean> = _isProviderLoggedIn.asStateFlow()

    var currentProviderId = ""
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

    private val _providerProfile = MutableStateFlow<Provider?>(null)
    val providerProfile: StateFlow<Provider?> = _providerProfile.asStateFlow()

    private val _incomingRequests = MutableStateFlow<List<Booking>>(emptyList())
    val incomingRequests: StateFlow<List<Booking>> = _incomingRequests.asStateFlow()

    private val _activeJobs = MutableStateFlow<List<Booking>>(emptyList())
    val activeJobs: StateFlow<List<Booking>> = _activeJobs.asStateFlow()

    private val _completedJobs = MutableStateFlow<List<Booking>>(emptyList())
    val completedJobs: StateFlow<List<Booking>> = _completedJobs.asStateFlow()

    private val _cancelledJobs = MutableStateFlow<List<Booking>>(emptyList())
    val cancelledJobs: StateFlow<List<Booking>> = _cancelledJobs.asStateFlow()

    private val _servicesForCategories = MutableStateFlow<Map<String, List<com.example.homeserve.data.model.ServiceModel>>>(emptyMap())
    val servicesForCategories: StateFlow<Map<String, List<com.example.homeserve.data.model.ServiceModel>>> = _servicesForCategories.asStateFlow()

    // Track bookings this provider has declined locally (they remain "pending" for others)
    private val declinedBookingIds = mutableSetOf<String>()
    private val locallyAcceptedBookingIds = mutableSetOf<String>()
    private val locallyCancelledBookingIds = mutableSetOf<String>()
    private val locallyCompletedBookingIds = mutableSetOf<String>()
    private var wasAvailable: Boolean? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasUnreadJobs = MutableStateFlow(false)
    val hasUnreadJobs: StateFlow<Boolean> = _hasUnreadJobs.asStateFlow()

    private val _notifications = MutableStateFlow<List<com.example.homeserve.data.model.AppNotification>>(emptyList())
    val notifications: StateFlow<List<com.example.homeserve.data.model.AppNotification>> = _notifications.asStateFlow()
    private var notificationsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    private val _lastMessages = MutableStateFlow<Map<String, ChatMessage>>(emptyMap())
    val lastMessages: StateFlow<Map<String, ChatMessage>> = _lastMessages.asStateFlow()

    private val _chatThreads = MutableStateFlow<List<com.example.homeserve.data.model.ChatThread>>(emptyList())
    val chatThreads: StateFlow<List<com.example.homeserve.data.model.ChatThread>> = _chatThreads.asStateFlow()

    fun startListeningToNotifications() {
        if (currentProviderId.isEmpty()) return
        notificationsListenerRegistration?.remove()
        notificationsListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("recipientId", currentProviderId)
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

    fun insertNotification(title: String, message: String, targetScreen: String = "provider_jobs") {
        viewModelScope.launch {
            val notif = com.example.homeserve.data.model.AppNotification(
                recipientId = currentProviderId,
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
        prefs.edit().putBoolean("unread_chat_${currentProviderId}_${bookingId}", false).apply()
        updateUnreadCount()
        rebuildChatThreads()
    }

    fun clearJobsUpdateBadge() {
        val ctx = com.example.homeserve.HomeServeApp.getContext()
        val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("unread_status_update_${currentProviderId}", false).apply()
        updateUnreadCount()
    }

    fun updateUnreadCount() {
        val ctx = com.example.homeserve.HomeServeApp.getContext()
        val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
        val activeBookingIds = _activeJobs.value
            .map { it.bookingId }
        
        var hasUnread = prefs.getBoolean("unread_status_update_${currentProviderId}", false)
        if (!hasUnread) {
            for (id in activeBookingIds) {
                if (prefs.getBoolean("unread_chat_${currentProviderId}_${id}", false)) {
                    hasUnread = true
                    break
                }
            }
        }
        _hasUnreadJobs.value = hasUnread
    }

    private var providerListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var incomingRequestsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var assignedJobsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    // Temporary registration/wizard state
    var tempFullName = ""
    var tempPhone = ""
    var tempAddress = ""
    var tempIdNumber = ""
    var tempRadiusKm = 5
    var tempLatitude = 0.0
    var tempLongitude = 0.0
    var tempProfilePhotoUrl = ""
    var tempDocumentUrl = ""


    fun signInWithGoogle(email: String, name: String, onCheckResult: (Boolean) -> Unit) {
        loggedInPhone = email
        currentProviderId = "google_prov_${email.replace("[^a-zA-Z0-9]".toRegex(), "_")}"
        _isProviderLoggedIn.value = true
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

                val profile = repository.getProviderProfile(currentProviderId)
                _providerProfile.value = profile
                _isLoading.value = false
                if (profile != null) {
                    loadProviderData()
                    startListeningToNotifications()
                    onCheckResult(true)
                } else {
                    onCheckResult(false)
                }
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
                    currentProviderId = "email_prov_${uid}"
                    _isProviderLoggedIn.value = true
                    viewModelScope.launch {
                        _isLoading.value = true
                        try {
                            val profile = repository.getProviderProfile(currentProviderId)
                            _providerProfile.value = profile
                            _isLoading.value = false
                            if (profile != null) {
                                loadProviderData()
                                startListeningToNotifications()
                                onResult(true, null)
                            } else {
                                // New provider who signed in via email — send to profile setup
                                onResult(true, null)
                            }
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
                    currentProviderId = "email_prov_${uid}"
                    _isProviderLoggedIn.value = true
                    // Store minimal info in temp fields for profile setup
                    tempFullName = name
                    _isLoading.value = false
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message ?: "Registration failed.")
                }
            }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean) -> Unit) {
        com.google.firebase.auth.FirebaseAuth.getInstance()
            .sendPasswordResetEmail(email)
            .addOnCompleteListener { task -> onResult(task.isSuccessful) }
    }

    // ─────────────────────────────────────────────────────────────────────────

    fun signInWithPhoneAndPin(
        phone: String,
        pin: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        loggedInPhone = phone
        currentProviderId = "provider_${phone.replace("[^0-9]".toRegex(), "")}"
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = repository.getProviderProfile(currentProviderId)
                if (profile == null) {
                    _isLoading.value = false
                    onComplete(false, "Account not found. Please register first.")
                    return@launch
                }
                
                if (profile.password != pin) {
                    _isLoading.value = false
                    onComplete(false, "Incorrect PIN code. Please try again.")
                    return@launch
                }
                
                _isProviderLoggedIn.value = true
                _providerProfile.value = profile
                loadProviderData()
                startListeningToNotifications()
                saveFcmTokenForCurrentProvider()
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
        pin: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        loggedInPhone = phone
        currentProviderId = "provider_${phone.replace("[^0-9]".toRegex(), "")}"
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = repository.getProviderProfile(currentProviderId)
                if (profile != null) {
                    _isLoading.value = false
                    onComplete(false, "An account with this phone number already exists.")
                    return@launch
                }
                
                val newProvider = Provider(
                    uid = currentProviderId,
                    name = name,
                    phone = phone,
                    password = pin,
                    role = "provider"
                )
                
                val success = repository.saveProviderProfile(newProvider)
                if (success) {
                    _isProviderLoggedIn.value = true
                    _providerProfile.value = newProvider
                    loadProviderData()
                    startListeningToNotifications()
                    saveFcmTokenForCurrentProvider()
                    _isLoading.value = false
                    onComplete(true, null)
                } else {
                    _isLoading.value = false
                    onComplete(false, "Failed to create account. Please try again.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                onComplete(false, e.message ?: "Registration failed.")
            }
        }
    }

    fun saveFcmTokenForCurrentProvider() {
        if (currentProviderId.isEmpty()) return
        viewModelScope.launch {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        viewModelScope.launch {
                            repository.saveFcmToken(currentProviderId, token, "provider")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setProviderId(phone: String, onCheckResult: (Boolean) -> Unit) {
        loggedInPhone = phone
        currentProviderId = "provider_${phone.replace("[^0-9]".toRegex(), "")}"
        _isProviderLoggedIn.value = true
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = repository.getProviderProfile(currentProviderId)
                _providerProfile.value = profile
                _isLoading.value = false
                if (profile != null) {
                    // If profile exists, listen to it in real-time
                    loadProviderData()
                    startListeningToNotifications()
                    onCheckResult(true)
                } else {
                    onCheckResult(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                onCheckResult(false)
            }
        }
    }

    fun loadProviderData() {
        if (!_isProviderLoggedIn.value) return
        providerListenerRegistration?.remove()
        
        _isLoading.value = true
        providerListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("providers")
            .document(currentProviderId)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (!_isProviderLoggedIn.value) return@addSnapshotListener
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val newProfile = snapshot.toObject(Provider::class.java)?.copy(uid = snapshot.id)
                    _providerProfile.value = newProfile

                    // Sync notification settings in real-time
                    try {
                        val ctx = com.example.homeserve.HomeServeApp.getContext()
                        val notifPrefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
                        
                        val bAlerts = snapshot.getBoolean("notification_pref_provider_booking_alerts") ?: true
                        val cAlerts = snapshot.getBoolean("notification_pref_provider_chat_alerts") ?: true
                        val mAlerts = snapshot.getBoolean("notification_pref_provider_marketing_alerts") ?: true
                        
                        notifPrefs.edit().apply {
                            putBoolean("notification_pref_provider_booking_alerts", bAlerts)
                            putBoolean("notification_pref_provider_chat_alerts", cAlerts)
                            putBoolean("notification_pref_provider_marketing_alerts", mAlerts)
                            apply()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (newProfile != null) {
                        try {
                            val ctx = com.example.homeserve.HomeServeApp.getContext()
                            val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
                            val wasPending = prefs.getBoolean("pending_approval_${newProfile.uid}", false)
                            
                            if (newProfile.isApproved) {
                                if (wasPending) {
                                    prefs.edit()
                                        .putBoolean("pending_approval_${newProfile.uid}", false)
                                        .apply()
                                    com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                        context = ctx,
                                        title = "Account Approved! 🚀",
                                        message = "Congratulations! Your profile has been approved. You can now accept jobs.",
                                        targetScreen = "provider_profile"
                                    )
                                }
                            } else if (newProfile.isRejected) {
                                if (wasPending) {
                                    prefs.edit()
                                        .putBoolean("pending_approval_${newProfile.uid}", false)
                                        .apply()
                                    com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                        context = ctx,
                                        title = "Verification Rejected ❌",
                                        message = "Your verification details were rejected. Please review and re-apply.",
                                        targetScreen = "provider_profile"
                                    )
                                }
                            } else {
                                // Account is neither approved nor rejected (pending review)
                                prefs.edit()
                                    .putBoolean("pending_approval_${newProfile.uid}", true)
                                    .apply()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        listenToBookings(newProfile)
                    }
                }
            }
    }

    fun stopListening() {
        providerListenerRegistration?.remove()
        incomingRequestsListenerRegistration?.remove()
        assignedJobsListenerRegistration?.remove()
        notificationsListenerRegistration?.remove()
        providerListenerRegistration = null
        incomingRequestsListenerRegistration = null
        assignedJobsListenerRegistration = null
        notificationsListenerRegistration = null
        _isProviderLoggedIn.value = false
        _providerProfile.value = null
        _incomingRequests.value = emptyList()
        _activeJobs.value = emptyList()
        _completedJobs.value = emptyList()
        _cancelledJobs.value = emptyList()
        _notifications.value = emptyList()
        _hasUnreadJobs.value = false
        locallyAcceptedBookingIds.clear()
        locallyCancelledBookingIds.clear()
        locallyCompletedBookingIds.clear()
        wasAvailable = null
        chatListeners.values.forEach { it.remove() }
        chatListeners.clear()
        _chatThreads.value = emptyList()
        _lastMessages.value = emptyMap()
    }

    override fun onCleared() {
        super.onCleared()
        providerListenerRegistration?.remove()
        incomingRequestsListenerRegistration?.remove()
        assignedJobsListenerRegistration?.remove()
        notificationsListenerRegistration?.remove()
        chatListeners.values.forEach { it.remove() }
        chatListeners.clear()
    }

    private fun stopBookingListenersOnly() {
        incomingRequestsListenerRegistration?.remove()
        incomingRequestsListenerRegistration = null
        assignedJobsListenerRegistration?.remove()
        assignedJobsListenerRegistration = null
    }

    fun listenToBookings(profile: Provider) {
        if (!_isProviderLoggedIn.value) {
            stopBookingListenersOnly()
            return
        }

        if (!profile.isApproved) {
            stopBookingListenersOnly()
            _incomingRequests.value = emptyList()
            _activeJobs.value = emptyList()
            _completedJobs.value = emptyList()
            _cancelledJobs.value = emptyList()
            return
        }

        val availabilityChanged = wasAvailable != null && wasAvailable != profile.isAvailable
        wasAvailable = profile.isAvailable

        if (availabilityChanged && profile.isAvailable) {
            stopBookingListenersOnly()
        }

        if (incomingRequestsListenerRegistration != null && assignedJobsListenerRegistration != null) {
            return
        }

        // 1. Listen to incoming requests (pending)
        if (incomingRequestsListenerRegistration == null) {
            val baseQuery = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("status", "pending")
                .whereEqualTo("providerId", "")

            val prefix = if (profile.providerLatitude != 0.0 && profile.providerLongitude != 0.0 && profile.radiusKm > 0) {
                com.example.homeserve.data.GeohashUtils.getCommonGeohashPrefix(
                    profile.providerLatitude,
                    profile.providerLongitude,
                    profile.radiusKm.toDouble()
                )
            } else {
                ""
            }

            val finalQuery = if (prefix.isNotEmpty()) {
                baseQuery.orderBy("geohash")
                    .startAt(prefix)
                    .endAt(prefix + "\uf8ff")
            } else {
                baseQuery
            }

            incomingRequestsListenerRegistration = finalQuery
                .addSnapshotListener { snapshot, error ->
                    if (!_isProviderLoggedIn.value) return@addSnapshotListener
                    if (error != null) {
                        error.printStackTrace()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val currentProfile = _providerProfile.value ?: return@addSnapshotListener
                        val categoryIds = currentProfile.categoryId.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        try {
                            val ctx = com.example.homeserve.HomeServeApp.getContext()
                            if (currentProfile.isAvailable) {
                                val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
                                var lastActiveTime = prefs.getLong("last_active_${profile.uid}_incoming", 0L)
                                if (lastActiveTime == 0L) {
                                    lastActiveTime = System.currentTimeMillis() - 5000
                                    prefs.edit().putLong("last_active_${profile.uid}_incoming", lastActiveTime).apply()
                                }

                                for (change in snapshot.documentChanges) {
                                    val booking = change.document.toObject(Booking::class.java).copy(bookingId = change.document.id)
                                    if (booking.categoryId in categoryIds) {
                                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                            val createdAtMs = booking.createdAt.toDate().time
                                            val notKey = "notified_${profile.uid}_${booking.bookingId}_new_request"
                                            if (createdAtMs > lastActiveTime - 10000 && !prefs.getBoolean(notKey, false)) {
                                                prefs.edit().putBoolean(notKey, true).apply()
                                                prefs.edit().putBoolean("unread_status_update_${currentProviderId}", true).apply()
                                                com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                    context = ctx,
                                                    title = "New Job Request Available! 🔔",
                                                    message = "A customer has requested a new ${booking.serviceName.ifBlank { "Home Service" }}.",
                                                    targetScreen = "provider_jobs"
                                                )
                                                insertNotification("New Job Request Available! 🔔", "A customer has requested a new ${booking.serviceName.ifBlank { "Home Service" }}.", "provider_jobs")
                                            }
                                        }
                                    }
                                }
                                prefs.edit().putLong("last_active_${profile.uid}_incoming", System.currentTimeMillis()).apply()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        viewModelScope.launch {
                            val bookings = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(Booking::class.java)?.copy(bookingId = doc.id)
                            }.filter { it.categoryId in categoryIds }.sortedByDescending { it.createdAt }

                            val resolvedBookings = bookings.map { booking ->
                                if (booking.customerPhone.isBlank() || booking.customerName.isBlank()) {
                                    try {
                                        val userSnapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            .collection("users").document(booking.userId).get().await()
                                        if (userSnapshot.exists()) {
                                            val name = userSnapshot.getString("name") ?: "Customer"
                                            val phone = userSnapshot.getString("phone") ?: ""
                                            booking.copy(customerName = name, customerPhone = phone)
                                        } else {
                                            booking
                                        }
                                    } catch (e: Exception) {
                                        booking
                                    }
                                } else {
                                    booking
                                }
                            }

                            val filteredPending = resolvedBookings.filter { booking ->
                                val isNotDeclined = booking.bookingId !in declinedBookingIds
                                val isNotAcceptedLocally = booking.bookingId !in locallyAcceptedBookingIds
                                
                                val isWithinRadius = if (currentProfile.providerLatitude != 0.0 && currentProfile.providerLongitude != 0.0 &&
                                    booking.customerLatitude != 0.0 && booking.customerLongitude != 0.0) {
                                    val distance = LocationUtils.getDistanceInKm(
                                        currentProfile.providerLatitude, currentProfile.providerLongitude,
                                        booking.customerLatitude, booking.customerLongitude
                                    )
                                    distance <= currentProfile.radiusKm
                                } else {
                                    true
                                }

                                val matchesServices = if (currentProfile.selectedServiceIds.isNotBlank()) {
                                    val serviceIds = currentProfile.selectedServiceIds.split(",").map { it.trim() }
                                    booking.serviceId in serviceIds
                                } else {
                                    true
                                }

                                isNotDeclined && isNotAcceptedLocally && isWithinRadius && matchesServices
                            }
                            _incomingRequests.value = filteredPending
                            updateUnreadCount()
                        }
                    }
                }
        }

        // 2. Listen to assigned jobs
        if (assignedJobsListenerRegistration == null) {
            assignedJobsListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("providerId", profile.uid)
                .addSnapshotListener { snapshot, error ->
                    if (!_isProviderLoggedIn.value) return@addSnapshotListener
                    if (error != null) {
                        error.printStackTrace()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val currentProfile = _providerProfile.value
                        try {
                            val ctx = com.example.homeserve.HomeServeApp.getContext()
                            if (currentProfile != null && currentProfile.isAvailable) {
                                val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
                                var lastActiveTime = prefs.getLong("last_active_${profile.uid}_assigned", 0L)
                                if (lastActiveTime == 0L) {
                                    lastActiveTime = System.currentTimeMillis() - 5000
                                    prefs.edit().putLong("last_active_${profile.uid}_assigned", lastActiveTime).apply()
                                }

                                for (change in snapshot.documentChanges) {
                                    val booking = change.document.toObject(Booking::class.java).copy(bookingId = change.document.id)
                                    val serviceName = booking.serviceName.ifBlank { "Service" }
                                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                        val updatedAtMs = booking.updatedAt.toDate().time
                                        if (updatedAtMs > lastActiveTime - 10000) {
                                            if (booking.status == "accepted" && booking.bookingId !in locallyAcceptedBookingIds) {
                                                val notKey = "notified_${profile.uid}_${booking.bookingId}_assigned"
                                                if (!prefs.getBoolean(notKey, false)) {
                                                    prefs.edit().putBoolean(notKey, true).apply()
                                                    prefs.edit().putBoolean("unread_status_update_${currentProviderId}", true).apply()
                                                    com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                        context = ctx,
                                                        title = "New Job Assigned! 💼",
                                                        message = "You have been assigned to $serviceName.",
                                                        targetScreen = "provider_jobs"
                                                    )
                                                    insertNotification("New Job Assigned! 💼", "You have been assigned to $serviceName.", "provider_jobs")
                                                }
                                            } else if (booking.status == "cancelled" && booking.bookingId !in locallyCancelledBookingIds) {
                                                val notKey = "notified_${profile.uid}_${booking.bookingId}_cancelled"
                                                if (!prefs.getBoolean(notKey, false)) {
                                                    prefs.edit().putBoolean(notKey, true).apply()
                                                    prefs.edit().putBoolean("unread_status_update_${currentProviderId}", true).apply()
                                                    com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                        context = ctx,
                                                        title = "Job Cancelled ❌",
                                                        message = "The job request for $serviceName has been cancelled by the customer.",
                                                        targetScreen = "provider_jobs"
                                                    )
                                                    insertNotification("Job Cancelled ❌", "The job request for $serviceName has been cancelled by the customer.", "provider_jobs")
                                                }
                                            } else if (booking.status == "completed" && booking.bookingId !in locallyCompletedBookingIds) {
                                                val notKey = "notified_${profile.uid}_${booking.bookingId}_completed"
                                                if (!prefs.getBoolean(notKey, false)) {
                                                    prefs.edit().putBoolean(notKey, true).apply()
                                                    prefs.edit().putBoolean("unread_status_update_${currentProviderId}", true).apply()
                                                    com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                        context = ctx,
                                                        title = "Job Completed ✅",
                                                        message = "The customer has marked the job for $serviceName as completed.",
                                                        targetScreen = "provider_jobs"
                                                    )
                                                    insertNotification("Job Completed ✅", "The customer has marked the job for $serviceName as completed.", "provider_jobs")
                                                }
                                            }
                                        }
                                    } else if (change.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                                        if (booking.status == "cancelled" && booking.bookingId !in locallyCancelledBookingIds) {
                                            val updatedAtMs = booking.updatedAt.toDate().time
                                            val notKey = "notified_${profile.uid}_${booking.bookingId}_cancelled"
                                            if (updatedAtMs > lastActiveTime - 10000 && !prefs.getBoolean(notKey, false)) {
                                                prefs.edit().putBoolean(notKey, true).apply()
                                                prefs.edit().putBoolean("unread_status_update_${currentProviderId}", true).apply()
                                                com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                    context = ctx,
                                                    title = "Job Cancelled ❌",
                                                    message = "The job request for $serviceName has been cancelled by the customer.",
                                                    targetScreen = "provider_jobs"
                                                )
                                                insertNotification("Job Cancelled ❌", "The job request for $serviceName has been cancelled by the customer.", "provider_jobs")
                                            }
                                        } else if (booking.status == "completed" && booking.bookingId !in locallyCompletedBookingIds) {
                                            val updatedAtMs = booking.updatedAt.toDate().time
                                            val notKey = "notified_${profile.uid}_${booking.bookingId}_completed"
                                            if (updatedAtMs > lastActiveTime - 10000 && !prefs.getBoolean(notKey, false)) {
                                                prefs.edit().putBoolean(notKey, true).apply()
                                                prefs.edit().putBoolean("unread_status_update_${currentProviderId}", true).apply()
                                                com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                    context = ctx,
                                                    title = "Job Completed ✅",
                                                    message = "The customer has marked the job for $serviceName as completed.",
                                                    targetScreen = "provider_jobs"
                                                )
                                                insertNotification("Job Completed ✅", "The customer has marked the job for $serviceName as completed.", "provider_jobs")
                                            }
                                        }
                                    }
                                }
                                prefs.edit().putLong("last_active_${profile.uid}_assigned", System.currentTimeMillis()).apply()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        viewModelScope.launch {
                            val bookings = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(Booking::class.java)?.copy(bookingId = doc.id)
                            }.sortedByDescending { it.createdAt }

                            val resolvedBookings = bookings.map { booking ->
                                if (booking.customerPhone.isBlank() || booking.customerName.isBlank()) {
                                    try {
                                        val userSnapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            .collection("users").document(booking.userId).get().await()
                                        if (userSnapshot.exists()) {
                                            val name = userSnapshot.getString("name") ?: "Customer"
                                            val phone = userSnapshot.getString("phone") ?: ""
                                            booking.copy(customerName = name, customerPhone = phone)
                                        } else {
                                            booking
                                        }
                                    } catch (e: Exception) {
                                        booking
                                    }
                                } else {
                                    booking
                                }
                            }

                             _activeJobs.value = resolvedBookings.filter { 
                                 (it.status == "accepted" || it.status == "in_progress") && 
                                 it.bookingId !in locallyCancelledBookingIds && 
                                 it.bookingId !in locallyCompletedBookingIds
                             }
                             _completedJobs.value = resolvedBookings.filter { 
                                 it.status == "completed" || it.bookingId in locallyCompletedBookingIds 
                             }
                             _cancelledJobs.value = resolvedBookings.filter { 
                                 it.status == "cancelled" || it.bookingId in locallyCancelledBookingIds 
                             }
                            syncChatListeners(profile.uid, resolvedBookings)
                            updateUnreadCount()
                            rebuildChatThreads()
                        }
                    }
                }
        }
    }

    fun fetchBookings(profile: Provider) {
        listenToBookings(profile)
    }

    fun saveTempProfileInfo(
        name: String,
        phone: String,
        idNumber: String,
        address: String,
        radiusKm: Int,
        latitude: Double,
        longitude: Double,
        photoUrl: String,
        docUrl: String
    ) {
        tempFullName = name
        tempPhone = phone
        tempIdNumber = idNumber
        tempAddress = address
        tempRadiusKm = radiusKm
        tempLatitude = latitude
        tempLongitude = longitude
        tempProfilePhotoUrl = photoUrl
        tempDocumentUrl = docUrl
    }

    fun loadServicesForCategories(categoryIds: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            val servicesMap = mutableMapOf<String, List<com.example.homeserve.data.model.ServiceModel>>()
            for (catId in categoryIds) {
                val list = repository.getServicesByCategory(catId)
                servicesMap[catId] = list
            }
            _servicesForCategories.value = servicesMap
            _isLoading.value = false
        }
    }

    fun saveProviderProfileWithCategoriesAndServices(
        context: android.content.Context,
        selectedCategories: List<String>,
        selectedServices: List<String>,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val currentProfile = _providerProfile.value
            val categoryIdsString = selectedCategories.joinToString(",")
            val serviceIdsString = selectedServices.joinToString(",")
            
            val newProvider = if (currentProfile != null) {
                val storageRepo = com.example.homeserve.data.StorageRepository()
                
                var finalPhotoUrl = if (tempProfilePhotoUrl.isNotBlank()) {
                    if (tempProfilePhotoUrl.startsWith("http")) tempProfilePhotoUrl else {
                        val uploadedPhoto = storageRepo.uploadFile(
                            context, 
                            android.net.Uri.parse(tempProfilePhotoUrl), 
                            "provider_photos/$currentProviderId"
                        )
                        uploadedPhoto ?: currentProfile.profilePhotoUrl
                    }
                } else currentProfile.profilePhotoUrl

                var finalDocUrl = if (tempDocumentUrl.isNotBlank()) {
                    if (tempDocumentUrl.startsWith("http")) tempDocumentUrl else {
                        val uploadedDoc = storageRepo.uploadFile(
                            context, 
                            android.net.Uri.parse(tempDocumentUrl), 
                            "provider_documents/$currentProviderId"
                        )
                        uploadedDoc ?: currentProfile.documentUrl
                    }
                } else currentProfile.documentUrl

                currentProfile.copy(
                    name = if (tempFullName.isNotEmpty()) tempFullName else currentProfile.name,
                    phone = if (tempPhone.isNotEmpty()) tempPhone else currentProfile.phone,
                    categoryId = categoryIdsString,
                    selectedServiceIds = serviceIdsString,
                    radiusKm = if (tempRadiusKm > 0) tempRadiusKm else currentProfile.radiusKm,
                    providerLatitude = if (tempLatitude != 0.0) tempLatitude else currentProfile.providerLatitude,
                    providerLongitude = if (tempLongitude != 0.0) tempLongitude else currentProfile.providerLongitude,
                    address = if (tempAddress.isNotEmpty()) tempAddress else currentProfile.address,
                    profilePhotoUrl = finalPhotoUrl,
                    documentUrl = finalDocUrl,
                    idNumber = if (tempIdNumber.isNotEmpty()) tempIdNumber else currentProfile.idNumber
                )
            } else {
                val storageRepo = com.example.homeserve.data.StorageRepository()
                
                // Upload profile photo if present
                var finalPhotoUrl = tempProfilePhotoUrl
                if (tempProfilePhotoUrl.isNotBlank()) {
                    val uploadedPhoto = storageRepo.uploadFile(
                        context, 
                        android.net.Uri.parse(tempProfilePhotoUrl), 
                        "provider_photos/$currentProviderId"
                    )
                    if (uploadedPhoto != null) {
                        finalPhotoUrl = uploadedPhoto
                    }
                }
                
                // Upload document if present
                var finalDocUrl = tempDocumentUrl
                if (tempDocumentUrl.isNotBlank()) {
                    val uploadedDoc = storageRepo.uploadFile(
                        context, 
                        android.net.Uri.parse(tempDocumentUrl), 
                        "provider_documents/$currentProviderId"
                    )
                    if (uploadedDoc != null) {
                        finalDocUrl = uploadedDoc
                    }
                }

                Provider(
                    uid = currentProviderId,
                    name = if (tempFullName.isNotEmpty()) tempFullName else "Service Provider",
                    phone = if (tempPhone.isNotEmpty()) tempPhone else (if (loggedInPhone.contains("@")) "" else loggedInPhone),
                    email = if (loggedInPhone.contains("@")) loggedInPhone else "",
                    categoryId = categoryIdsString,
                    selectedServiceIds = serviceIdsString,
                    bio = "Professional home service provider",
                    rating = 4.8,
                    reviewCount = 12,
                    isAvailable = true,
                    isApproved = false, // Require admin approval by default
                    isRejected = false,
                    radiusKm = tempRadiusKm,
                    providerLatitude = tempLatitude,
                    providerLongitude = tempLongitude,
                    address = if (tempAddress.isNotEmpty()) tempAddress else "Lahore, PK",
                    profilePhotoUrl = finalPhotoUrl,
                    documentUrl = finalDocUrl,
                    idNumber = tempIdNumber
                )
            }

            val success = repository.saveProviderProfile(newProvider)
            if (success) {
                try {
                    val ctx = com.example.homeserve.HomeServeApp.getContext()
                    val notifTitle = if (currentProfile != null) "Profile Updated! 📝" else "Profile Saved! 📝"
                    val notifMsg = if (currentProfile != null) "Your service categories have been updated successfully." else "Your profile has been submitted for admin verification."
                    com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                        context = ctx,
                        title = notifTitle,
                        message = notifMsg,
                        targetScreen = "provider_profile"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _providerProfile.value = newProvider
            fetchBookings(newProvider)
            _isLoading.value = false
            onComplete()
        }
    }

    fun updateProviderAddress(address: String, radiusKm: Int, latitude: Double, longitude: Double) {
        val currentProfile = _providerProfile.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val updated = currentProfile.copy(
                address = address,
                radiusKm = radiusKm,
                providerLatitude = latitude,
                providerLongitude = longitude
            )
            repository.saveProviderProfile(updated)
            _providerProfile.value = updated
            fetchBookings(updated)
            _isLoading.value = false
        }
    }

    fun updateProviderProfile(
        name: String,
        email: String,
        bio: String,
        address: String,
        radiusKm: Int,
        latitude: Double,
        longitude: Double,
        passwordVal: String,
        context: android.content.Context? = null,
        newPhotoUri: android.net.Uri? = null,
        onComplete: (Boolean) -> Unit
    ) {
        val currentProfile = _providerProfile.value ?: run {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            // Upload new profile photo if provided
            var finalPhotoUrl = currentProfile.profilePhotoUrl
            if (newPhotoUri != null && context != null) {
                try {
                    val storageRepo = com.example.homeserve.data.StorageRepository()
                    val uploaded = storageRepo.uploadFile(
                        context,
                        newPhotoUri,
                        "provider_photos/$currentProviderId"
                    )
                    if (uploaded != null) finalPhotoUrl = uploaded
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val updated = currentProfile.copy(
                name = name,
                email = email,
                bio = bio,
                address = address,
                radiusKm = radiusKm,
                providerLatitude = latitude,
                providerLongitude = longitude,
                password = passwordVal,
                profilePhotoUrl = finalPhotoUrl
            )
            val success = repository.saveProviderProfile(updated)
            if (success) {
                _providerProfile.value = updated
                fetchBookings(updated)
            }
            _isLoading.value = false
            onComplete(success)
        }
    }

    fun updateProviderLiveLocation(latitude: Double, longitude: Double) {
        val currentProfile = _providerProfile.value ?: return
        if (currentProfile.providerLatitude == latitude && currentProfile.providerLongitude == longitude) {
            return
        }
        viewModelScope.launch {
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("providers")
                    .document(currentProviderId)
                    .update(
                        mapOf(
                            "providerLatitude" to latitude,
                            "providerLongitude" to longitude
                        )
                    )
                val updated = currentProfile.copy(
                    providerLatitude = latitude,
                    providerLongitude = longitude
                )
                _providerProfile.value = updated
                fetchBookings(updated)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleAvailability(isAvailable: Boolean) {
        val currentProfile = _providerProfile.value ?: return
        viewModelScope.launch {
            val success = repository.updateProviderAvailability(currentProviderId, isAvailable)
            if (success) {
                _providerProfile.value = currentProfile.copy(isAvailable = isAvailable)
            }
        }
    }

    fun acceptJob(bookingId: String, price: Int) {
        if (_activeJobs.value.isNotEmpty()) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                val ctx = com.example.homeserve.HomeServeApp.getContext()
                android.widget.Toast.makeText(
                    ctx,
                    "You can only accept one active job at a time. Please complete or cancel your ongoing job first.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        locallyAcceptedBookingIds.add(bookingId)
        try {
            val ctx = com.example.homeserve.HomeServeApp.getContext()
            val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("notified_${currentProviderId}_${bookingId}_assigned", true).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Optimistically update lists to reflect status change immediately in the UI
        val job = _incomingRequests.value.find { it.bookingId == bookingId }
        if (job != null) {
            val accepted = job.copy(status = "accepted", providerId = currentProviderId, totalAmount = price)
            _incomingRequests.value = _incomingRequests.value.filter { it.bookingId != bookingId }
            _activeJobs.value = (listOf(accepted) + _activeJobs.value).distinctBy { it.bookingId }
        }

        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.updateBookingStatus(bookingId, "accepted", currentProviderId, price = price)
            if (success) {
                val profile = _providerProfile.value
                if (profile != null) {
                    fetchBookings(profile)
                }
            } else {
                // Revert optimistic updates
                val profile = _providerProfile.value
                if (profile != null) {
                    fetchBookings(profile)
                }
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    val ctx = com.example.homeserve.HomeServeApp.getContext()
                    android.widget.Toast.makeText(
                        ctx,
                        "This job was already accepted by another provider or is no longer pending.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            _isLoading.value = false
        }
    }



    fun declineJob(bookingId: String) {
        declinedBookingIds.add(bookingId)
        _incomingRequests.value = _incomingRequests.value.filter { it.bookingId != bookingId }
    }

    fun cancelJob(bookingId: String) {
        locallyCancelledBookingIds.add(bookingId)
        try {
            val ctx = com.example.homeserve.HomeServeApp.getContext()
            val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("notified_${currentProviderId}_${bookingId}_cancelled", true).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Optimistically update lists to reflect status change immediately in the UI
        val job = _activeJobs.value.find { it.bookingId == bookingId }
        if (job != null) {
            val cancelled = job.copy(status = "cancelled")
            _activeJobs.value = _activeJobs.value.filter { it.bookingId != bookingId }
            _cancelledJobs.value = (listOf(cancelled) + _cancelledJobs.value).distinctBy { it.bookingId }
        }

        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.updateBookingStatus(bookingId, "pending")
            if (success) {
                val profile = _providerProfile.value
                if (profile != null) {
                    fetchBookings(profile)
                }
            }
            _isLoading.value = false
        }
    }

    fun updateBookingPrice(bookingId: String, price: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.updateBookingPrice(bookingId, price)
            if (success) {
                val profile = _providerProfile.value
                if (profile != null) {
                    fetchBookings(profile)
                }
            }
            _isLoading.value = false
        }
    }

    fun updateJobStatus(bookingId: String, status: String) {
        if (status == "completed") {
            locallyCompletedBookingIds.add(bookingId)
            try {
                val ctx = com.example.homeserve.HomeServeApp.getContext()
                val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putBoolean("notified_${currentProviderId}_${bookingId}_completed", true).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Optimistically update lists to reflect status change immediately in the UI
            val job = _activeJobs.value.find { it.bookingId == bookingId }
            if (job != null) {
                val completed = job.copy(status = "completed")
                _activeJobs.value = _activeJobs.value.filter { it.bookingId != bookingId }
                _completedJobs.value = (listOf(completed) + _completedJobs.value).distinctBy { it.bookingId }
            }
        }
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.updateBookingStatus(bookingId, status)
            if (success) {
                val profile = _providerProfile.value
                if (profile != null) {
                    fetchBookings(profile)
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Marks a completed job as payment received by the provider.
     * Optimistically updates the local completed jobs list so the UI
     * reflects the change immediately without waiting for a Firestore refresh.
     */
    fun markPaymentReceived(bookingId: String, onResult: (Boolean) -> Unit = {}) {
        // Optimistic local update
        _completedJobs.value = _completedJobs.value.map { job ->
            if (job.bookingId == bookingId) job.copy(paymentStatus = "paid") else job
        }
        viewModelScope.launch {
            val success = repository.updatePaymentStatus(bookingId, "paid")
            onResult(success)
            if (!success) {
                // Revert optimistic update on failure
                _completedJobs.value = _completedJobs.value.map { job ->
                    if (job.bookingId == bookingId) job.copy(paymentStatus = "unpaid") else job
                }
            }
        }
    }

    fun rebuildChatThreads() {
        val activeList = _activeJobs.value
        val completedList = _completedJobs.value
        val cancelledList = _cancelledJobs.value
        val allBookings = activeList + completedList + cancelledList
        val lastMsgs = _lastMessages.value
        val ctx = com.example.homeserve.HomeServeApp.getContext()
        val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)

        val chatBookings = allBookings.filter { currentProviderId !in it.hiddenByUsers }

        val threads = chatBookings.map { booking ->
            val lastMsg = lastMsgs[booking.bookingId]
            val hasUnread = prefs.getBoolean("unread_chat_${currentProviderId}_${booking.bookingId}", false)
            
            com.example.homeserve.data.model.ChatThread(
                bookingId = booking.bookingId,
                otherPartyId = booking.userId,
                otherPartyName = booking.customerName.ifBlank { "Customer" },
                otherPartyPhotoUrl = "",
                lastMessageText = lastMsg?.text ?: "Tap to start conversation",
                lastMessageTimestamp = lastMsg?.timestamp ?: booking.updatedAt,
                unreadCount = if (hasUnread) 1 else 0
            )
        }.sortedByDescending { it.lastMessageTimestamp }
        
        _chatThreads.value = threads
    }

    private fun syncChatListeners(providerId: String, bookings: List<Booking>) {
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
        val nonActiveBookings = bookings.filter { it.bookingId !in activeBookingIds }
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
                val lastMsgKey = "last_msg_${providerId}_${bookingId}"
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
                        if (!_isProviderLoggedIn.value) return@addSnapshotListener
                        
                        val lastMsgTime = prefs.getLong("last_msg_${providerId}_${bookingId}", 0L)
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
                                if (msgTime > lastMsgTime && message.senderId != providerId) {
                                    val currentProfile = _providerProfile.value
                                    if (currentProfile != null && currentProfile.isAvailable && currentProfile.isApproved) {
                                        if (bookingId != ChatViewModel.activeChatBookingId) {
                                            val notKey = "notified_msg_${providerId}_${message.messageId}"
                                            if (!prefs.getBoolean(notKey, false)) {
                                                prefs.edit().putBoolean(notKey, true).apply()
                                                com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                    context = ctx,
                                                    title = "New Message from ${message.senderName} 💬",
                                                    message = message.text,
                                                    targetScreen = "chat/$bookingId/provider"
                                                )
                                                insertNotification("New Message from ${message.senderName} 💬", message.text, "chat/$bookingId/provider")
                                                prefs.edit().putBoolean("unread_chat_${providerId}_${bookingId}", true).apply()
                                                updateUnreadCount()
                                                rebuildChatThreads()
                                            }
                                        }
                                    }
                                }
                                if (msgTime > newLastTime) {
                                    newLastTime = msgTime
                                }
                            }
                        }
                        if (newLastTime > lastMsgTime) {
                            prefs.edit().putLong("last_msg_${providerId}_${bookingId}", newLastTime).apply()
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
            repository.clearAllNotifications(currentProviderId)
        }
    }

    fun deleteChatThread(bookingId: String) {
        viewModelScope.launch {
            repository.hideChatThread(bookingId, currentProviderId)
            rebuildChatThreads()
        }
    }

    fun deleteAccount(onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.deleteProviderHistoryAndProfile(currentProviderId)
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
}

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

    // Track bookings this provider has declined locally (they remain "pending" for others)
    private val declinedBookingIds = mutableSetOf<String>()
    private val locallyAcceptedBookingIds = mutableSetOf<String>()
    private val locallyCancelledBookingIds = mutableSetOf<String>()
    private val locallyCompletedBookingIds = mutableSetOf<String>()
    private var wasAvailable: Boolean? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
        providerListenerRegistration = null
        incomingRequestsListenerRegistration = null
        assignedJobsListenerRegistration = null
        _isProviderLoggedIn.value = false
        _providerProfile.value = null
        _incomingRequests.value = emptyList()
        _activeJobs.value = emptyList()
        _completedJobs.value = emptyList()
        _cancelledJobs.value = emptyList()
        locallyAcceptedBookingIds.clear()
        locallyCancelledBookingIds.clear()
        locallyCompletedBookingIds.clear()
        wasAvailable = null
        chatListeners.values.forEach { it.remove() }
        chatListeners.clear()
    }

    override fun onCleared() {
        super.onCleared()
        providerListenerRegistration?.remove()
        incomingRequestsListenerRegistration?.remove()
        assignedJobsListenerRegistration?.remove()
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
            incomingRequestsListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("status", "pending")
                .whereEqualTo("providerId", "")
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
                                                com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                    context = ctx,
                                                    title = "New Job Request Available! 🔔",
                                                    message = "A customer has requested a new ${booking.serviceName.ifBlank { "Home Service" }}.",
                                                    targetScreen = "provider_jobs"
                                                )
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

                                isNotDeclined && isWithinRadius
                            }
                            _incomingRequests.value = filteredPending
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
                                                    com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                        context = ctx,
                                                        title = "New Job Assigned! 💼",
                                                        message = "You have been assigned to $serviceName.",
                                                        targetScreen = "provider_jobs"
                                                    )
                                                }
                                            } else if (booking.status == "cancelled" && booking.bookingId !in locallyCancelledBookingIds) {
                                                val notKey = "notified_${profile.uid}_${booking.bookingId}_cancelled"
                                                if (!prefs.getBoolean(notKey, false)) {
                                                    prefs.edit().putBoolean(notKey, true).apply()
                                                    com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                        context = ctx,
                                                        title = "Job Cancelled ❌",
                                                        message = "The job request for $serviceName has been cancelled by the customer.",
                                                        targetScreen = "provider_jobs"
                                                    )
                                                }
                                            } else if (booking.status == "completed" && booking.bookingId !in locallyCompletedBookingIds) {
                                                val notKey = "notified_${profile.uid}_${booking.bookingId}_completed"
                                                if (!prefs.getBoolean(notKey, false)) {
                                                    prefs.edit().putBoolean(notKey, true).apply()
                                                    com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                        context = ctx,
                                                        title = "Job Completed ✅",
                                                        message = "The customer has marked the job for $serviceName as completed.",
                                                        targetScreen = "provider_jobs"
                                                    )
                                                }
                                            }
                                        }
                                    } else if (change.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                                        if (booking.status == "cancelled" && booking.bookingId !in locallyCancelledBookingIds) {
                                            val updatedAtMs = booking.updatedAt.toDate().time
                                            val notKey = "notified_${profile.uid}_${booking.bookingId}_cancelled"
                                            if (updatedAtMs > lastActiveTime - 10000 && !prefs.getBoolean(notKey, false)) {
                                                prefs.edit().putBoolean(notKey, true).apply()
                                                com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                    context = ctx,
                                                    title = "Job Cancelled ❌",
                                                    message = "The job request for $serviceName has been cancelled by the customer.",
                                                    targetScreen = "provider_jobs"
                                                )
                                            }
                                        } else if (booking.status == "completed" && booking.bookingId !in locallyCompletedBookingIds) {
                                            val updatedAtMs = booking.updatedAt.toDate().time
                                            val notKey = "notified_${profile.uid}_${booking.bookingId}_completed"
                                            if (updatedAtMs > lastActiveTime - 10000 && !prefs.getBoolean(notKey, false)) {
                                                prefs.edit().putBoolean(notKey, true).apply()
                                                com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                                    context = ctx,
                                                    title = "Job Completed ✅",
                                                    message = "The customer has marked the job for $serviceName as completed.",
                                                    targetScreen = "provider_jobs"
                                                )
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

                            _activeJobs.value = resolvedBookings.filter { it.status == "accepted" || it.status == "in_progress" }
                            _completedJobs.value = resolvedBookings.filter { it.status == "completed" }
                            _cancelledJobs.value = resolvedBookings.filter { it.status == "cancelled" }
                            syncChatListeners(profile.uid, resolvedBookings)
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

    fun saveProviderProfileWithCategories(context: android.content.Context, selectedCategories: List<String>, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val storageRepo = com.example.homeserve.data.StorageRepository()
            
            // Upload profile photo if present
            var finalPhotoUrl = tempProfilePhotoUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150" }
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

            val categoryIdsString = selectedCategories.joinToString(",")
            val newProvider = Provider(
                uid = currentProviderId,
                name = if (tempFullName.isNotEmpty()) tempFullName else "Service Provider",
                phone = if (tempPhone.isNotEmpty()) tempPhone else (if (loggedInPhone.contains("@")) "" else loggedInPhone),
                email = if (loggedInPhone.contains("@")) loggedInPhone else "",
                categoryId = categoryIdsString,
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
            val success = repository.saveProviderProfile(newProvider)
            if (success) {
                try {
                    val ctx = com.example.homeserve.HomeServeApp.getContext()
                    com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                        context = ctx,
                        title = "Profile Saved! 📝",
                        message = "Your profile has been submitted for admin verification.",
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

    fun acceptJob(bookingId: String) {
        locallyAcceptedBookingIds.add(bookingId)
        try {
            val ctx = com.example.homeserve.HomeServeApp.getContext()
            val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("notified_${currentProviderId}_${bookingId}_assigned", true).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.updateBookingStatus(bookingId, "accepted", currentProviderId)
            if (success) {
                val profile = _providerProfile.value
                if (profile != null) {
                    fetchBookings(profile)
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
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.updateBookingStatus(bookingId, "cancelled")
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
}

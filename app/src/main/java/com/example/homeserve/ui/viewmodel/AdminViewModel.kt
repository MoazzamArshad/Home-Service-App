package com.example.homeserve.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeserve.data.FirestoreRepository
import com.example.homeserve.data.model.Booking
import com.example.homeserve.data.model.Provider
import com.example.homeserve.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {
    private val repository = FirestoreRepository()

    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _adminEmail = MutableStateFlow("admin@homeserve.com")
    val adminEmail: StateFlow<String> = _adminEmail.asStateFlow()

    private val _adminName = MutableStateFlow("Admin User")
    val adminName: StateFlow<String> = _adminName.asStateFlow()

    fun setLoggedInAdmin(email: String, name: String) {
        _adminEmail.value = email
        _adminName.value = name
        _isAdminLoggedIn.value = true
        loadDashboardData()
    }

    fun prepareAdminLogin(email: String, name: String) {
        _adminEmail.value = email
        _adminName.value = name
    }

    fun completeAdminLogin() {
        _isAdminLoggedIn.value = true
        loadDashboardData()
    }

    private val _totalUsers = MutableStateFlow(0)
    val totalUsers: StateFlow<Int> = _totalUsers.asStateFlow()

    private val _totalProviders = MutableStateFlow(0)
    val totalProviders: StateFlow<Int> = _totalProviders.asStateFlow()

    private val _totalBookings = MutableStateFlow(0)
    val totalBookings: StateFlow<Int> = _totalBookings.asStateFlow()

    private val _totalRevenue = MutableStateFlow(0)
    val totalRevenue: StateFlow<Int> = _totalRevenue.asStateFlow()

    private val _recentBookings = MutableStateFlow<List<Booking>>(emptyList())
    val recentBookings: StateFlow<List<Booking>> = _recentBookings.asStateFlow()

    private val _providersList = MutableStateFlow<List<Provider>>(emptyList())
    val providersList: StateFlow<List<Provider>> = _providersList.asStateFlow()

    private val _usersList = MutableStateFlow<List<User>>(emptyList())
    val usersList: StateFlow<List<User>> = _usersList.asStateFlow()

    private val _bookingsList = MutableStateFlow<List<Booking>>(emptyList())
    val bookingsList: StateFlow<List<Booking>> = _bookingsList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var providersListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var bookingsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var usersListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null



    fun loadDashboardData() {
        if (!_isAdminLoggedIn.value) return
        viewModelScope.launch {
            repository.seedDatabaseIfNeeded()
        }

        _isLoading.value = true

        val adminId = _adminEmail.value.replace("[^a-zA-Z0-9]".toRegex(), "_")

        // Listen to providers in real-time
        providersListenerRegistration?.remove()
        providersListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("providers")
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (!_isAdminLoggedIn.value) return@addSnapshotListener
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        val ctx = com.example.homeserve.HomeServeApp.getContext()
                        val prefs = ctx.getSharedPreferences("homeserve_notifications_prefs", android.content.Context.MODE_PRIVATE)
                        var lastActiveProviders = prefs.getLong("last_active_admin_${adminId}_providers", 0L)
                        if (lastActiveProviders == 0L) {
                            lastActiveProviders = System.currentTimeMillis() - 5000
                            prefs.edit().putLong("last_active_admin_${adminId}_providers", lastActiveProviders).apply()
                        }
                        
                        for (change in snapshot.documentChanges) {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                val provider = change.document.toObject(Provider::class.java).copy(uid = change.document.id)
                                val createdAtMs = provider.createdAt.toDate().time
                                val notKey = "notified_admin_${adminId}_${provider.uid}_registered"
                                if (!provider.isApproved && !provider.isRejected && createdAtMs > lastActiveProviders - 10000 && !prefs.getBoolean(notKey, false)) {
                                    prefs.edit().putBoolean(notKey, true).apply()
                                    com.example.homeserve.ui.notifications.NotificationHelper.showNotification(
                                        context = ctx,
                                        title = "Account Verification Pending 🔔",
                                        message = "${provider.name}'s account verification is pending.",
                                        targetScreen = "provider_approvals"
                                    )
                                }
                            }
                        }
                        prefs.edit().putLong("last_active_admin_${adminId}_providers", System.currentTimeMillis()).apply()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val allProviders = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Provider::class.java)?.copy(uid = doc.id)
                    }
                    _providersList.value = allProviders
                    _totalProviders.value = allProviders.size
                }
            }

        // Listen to bookings in real-time
        bookingsListenerRegistration?.remove()
        bookingsListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("bookings")
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (!_isAdminLoggedIn.value) return@addSnapshotListener
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val allBookings = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Booking::class.java)?.copy(bookingId = doc.id)
                    }.sortedByDescending { it.createdAt }

                    _bookingsList.value = allBookings
                    _totalBookings.value = allBookings.size
                    _recentBookings.value = allBookings.take(5)
                    _totalRevenue.value = allBookings.filter { it.status == "completed" }.sumOf { it.totalAmount }
                }
            }

        // Listen to users in real-time
        usersListenerRegistration?.remove()
        usersListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (snapshot != null) {
                    val allUsers = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(uid = doc.id)
                    }
                    _usersList.value = allUsers
                    _totalUsers.value = allUsers.size
                }
            }
    }

    fun approveProvider(providerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateProviderApprovalStatus(providerId, isApproved = true, isRejected = false)
            _isLoading.value = false
        }
    }

    fun rejectProvider(providerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateProviderApprovalStatus(providerId, isApproved = false, isRejected = true)
            _isLoading.value = false
        }
    }

    fun stopListening() {
        providersListenerRegistration?.remove()
        bookingsListenerRegistration?.remove()
        usersListenerRegistration?.remove()
        providersListenerRegistration = null
        bookingsListenerRegistration = null
        usersListenerRegistration = null
        _isAdminLoggedIn.value = false
        _providersList.value = emptyList()
        _bookingsList.value = emptyList()
        _usersList.value = emptyList()
        _recentBookings.value = emptyList()
        _totalUsers.value = 0
        _totalProviders.value = 0
        _totalBookings.value = 0
        _totalRevenue.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        providersListenerRegistration?.remove()
        bookingsListenerRegistration?.remove()
        usersListenerRegistration?.remove()
    }
}

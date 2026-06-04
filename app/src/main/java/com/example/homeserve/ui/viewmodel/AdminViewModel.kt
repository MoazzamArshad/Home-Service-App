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

    private val _categoriesList = MutableStateFlow<List<com.example.homeserve.data.model.Category>>(emptyList())
    val categoriesList: StateFlow<List<com.example.homeserve.data.model.Category>> = _categoriesList.asStateFlow()

    private val _servicesList = MutableStateFlow<List<com.example.homeserve.data.model.ServiceModel>>(emptyList())
    val servicesList: StateFlow<List<com.example.homeserve.data.model.ServiceModel>> = _servicesList.asStateFlow()

    private var categoriesListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var servicesListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var supportListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    private val _supportTickets = MutableStateFlow<List<com.example.homeserve.data.model.SupportTicket>>(emptyList())
    val supportTickets: StateFlow<List<com.example.homeserve.data.model.SupportTicket>> = _supportTickets.asStateFlow()



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

        // Listen to categories in real-time
        categoriesListenerRegistration?.remove()
        categoriesListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val allCategories = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.homeserve.data.model.Category::class.java)?.copy(categoryId = doc.id)
                    }
                    _categoriesList.value = allCategories
                }
            }

        // Listen to services in real-time
        servicesListenerRegistration?.remove()
        servicesListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("services")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val allServices = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.homeserve.data.model.ServiceModel::class.java)?.copy(serviceId = doc.id)
                    }
                    _servicesList.value = allServices
                }
            }

        // Listen to support tickets in real-time
        supportListenerRegistration?.remove()
        supportListenerRegistration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("support_tickets")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val tickets = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.homeserve.data.model.SupportTicket::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.createdAt }
                    _supportTickets.value = tickets
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
        categoriesListenerRegistration?.remove()
        servicesListenerRegistration?.remove()
        
        providersListenerRegistration = null
        bookingsListenerRegistration = null
        usersListenerRegistration = null
        categoriesListenerRegistration = null
        servicesListenerRegistration = null
        
        _isAdminLoggedIn.value = false
        _providersList.value = emptyList()
        _bookingsList.value = emptyList()
        _usersList.value = emptyList()
        _recentBookings.value = emptyList()
        _categoriesList.value = emptyList()
        _servicesList.value = emptyList()
        _supportTickets.value = emptyList()
        _totalUsers.value = 0
        _totalProviders.value = 0
        _totalBookings.value = 0
        _totalRevenue.value = 0
        supportListenerRegistration?.remove()
        supportListenerRegistration = null
    }

    override fun onCleared() {
        super.onCleared()
        providersListenerRegistration?.remove()
        bookingsListenerRegistration?.remove()
        usersListenerRegistration?.remove()
        categoriesListenerRegistration?.remove()
        servicesListenerRegistration?.remove()
        supportListenerRegistration?.remove()
    }

    // Category CRUD actions
    fun addCategory(name: String, icon: String) {
        viewModelScope.launch {
            val categoryId = "cat_" + System.currentTimeMillis().toString()
            val category = com.example.homeserve.data.model.Category(
                categoryId = categoryId,
                name = name,
                icon = icon,
                isActive = true
            )
            repository.addCategory(category)
        }
    }

    fun updateCategory(categoryId: String, name: String, icon: String, isActive: Boolean) {
        viewModelScope.launch {
            val category = com.example.homeserve.data.model.Category(
                categoryId = categoryId,
                name = name,
                icon = icon,
                isActive = isActive
            )
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
        }
    }

    // Sub-Service CRUD actions
    fun addService(categoryId: String, name: String, description: String, price: Int) {
        viewModelScope.launch {
            val serviceId = "srv_" + System.currentTimeMillis().toString()
            val service = com.example.homeserve.data.model.ServiceModel(
                serviceId = serviceId,
                categoryId = categoryId,
                name = name,
                description = description,
                price = price,
                isActive = true
            )
            repository.addService(service)
        }
    }

    fun updateService(serviceId: String, categoryId: String, name: String, description: String, price: Int, isActive: Boolean) {
        viewModelScope.launch {
            val service = com.example.homeserve.data.model.ServiceModel(
                serviceId = serviceId,
                categoryId = categoryId,
                name = name,
                description = description,
                price = price,
                isActive = isActive
            )
            repository.updateService(service)
        }
    }

    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            repository.deleteService(serviceId)
        }
    }

    fun updateUserBlockStatus(userId: String, isBlocked: Boolean) {
        viewModelScope.launch {
            repository.updateUserBlockStatus(userId, isBlocked)
        }
    }

    // Admin Settings
    private val _adminSettings = MutableStateFlow(com.example.homeserve.data.model.AdminSettings())
    val adminSettings: StateFlow<com.example.homeserve.data.model.AdminSettings> = _adminSettings.asStateFlow()

    private val _settingsLoading = MutableStateFlow(false)
    val settingsLoading: StateFlow<Boolean> = _settingsLoading.asStateFlow()

    private val _settingsSaved = MutableStateFlow(false)
    val settingsSaved: StateFlow<Boolean> = _settingsSaved.asStateFlow()

    fun loadAdminSettings() {
        val adminId = _adminEmail.value.replace("[^a-zA-Z0-9]".toRegex(), "_")
        viewModelScope.launch {
            _settingsLoading.value = true
            val settings = repository.getAdminSettings(adminId)
            if (settings != null) {
                _adminSettings.value = settings
            } else {
                // Create default settings for this admin
                _adminSettings.value = com.example.homeserve.data.model.AdminSettings(adminId = adminId)
            }
            _settingsLoading.value = false
        }
    }

    fun saveAdminSettings(settings: com.example.homeserve.data.model.AdminSettings) {
        val adminId = _adminEmail.value.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val updatedSettings = settings.copy(
            adminId = adminId,
            updatedAt = com.google.firebase.Timestamp.now()
        )
        // Optimistic UI state update
        _adminSettings.value = updatedSettings
        viewModelScope.launch {
            val success = repository.saveAdminSettings(updatedSettings)
            if (success) {
                _settingsSaved.value = true
            }
        }
    }

    fun clearSavedFlag() {
        _settingsSaved.value = false
    }

    fun updateTicketStatus(ticketId: String, status: String) {
        viewModelScope.launch {
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("support_tickets")
                    .document(ticketId)
                    .update("status", status)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                repository.updateBookingStatus(bookingId = bookingId, status = "cancelled")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

package com.example.homeserve.data

import com.example.homeserve.data.model.Booking
import com.example.homeserve.data.model.Category
import com.example.homeserve.data.model.Provider
import com.example.homeserve.data.model.ServiceModel
import com.example.homeserve.data.model.User
import com.example.homeserve.data.model.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    // Customer Side Functions
    suspend fun getCategories(): List<Category> {
        return try {
            val snapshot = db.collection("categories").get().await()
            snapshot.toObjects(Category::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getServicesByCategory(categoryId: String): List<ServiceModel> {
        return try {
            val snapshot = db.collection("services")
                .whereEqualTo("categoryId", categoryId)
                .get().await()
            snapshot.toObjects(ServiceModel::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getPopularServices(): List<ServiceModel> {
        return try {
            val snapshot = db.collection("services").limit(5).get().await()
            snapshot.toObjects(ServiceModel::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun createBooking(booking: Booking): Boolean {
        return try {
            val bookingWithGeohash = if (booking.customerLatitude != 0.0 && booking.customerLongitude != 0.0) {
                booking.copy(geohash = GeohashUtils.encode(booking.customerLatitude, booking.customerLongitude))
            } else {
                booking
            }
            db.collection("bookings").add(bookingWithGeohash).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getUserBookings(userId: String): List<Booking> {
        return try {
            val snapshot = db.collection("bookings")
                .whereEqualTo("userId", userId)
                .get().await()
            val bookings = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.copy(bookingId = doc.id)
            }.sortedByDescending { it.createdAt }
            
            bookings.map { booking ->
                if (booking.providerId.isNotBlank() && (booking.providerPhone.isBlank() || booking.providerName.isBlank())) {
                    try {
                        val providerSnapshot = db.collection("providers").document(booking.providerId).get().await()
                        if (providerSnapshot.exists()) {
                            val name = providerSnapshot.getString("name") ?: ""
                            val phone = providerSnapshot.getString("phone") ?: ""
                            booking.copy(providerName = name, providerPhone = phone)
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
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun seedDatabaseIfNeeded() {
        try {
            val categories = getCategories()
            if (categories.isEmpty()) {
                val mockCategories = com.example.homeserve.ui.data.CustomerMockData.serviceCategories
                val mockServices = com.example.homeserve.ui.data.CustomerMockData.services

                mockCategories.forEach { mockCat ->
                    val category = Category(
                        categoryId = mockCat.id,
                        name = mockCat.name,
                        icon = mockCat.icon,
                        isActive = true
                    )
                    db.collection("categories").document(mockCat.id).set(category).await()
                }

                mockServices.forEach { mockSrv ->
                    val service = ServiceModel(
                        serviceId = mockSrv.id,
                        categoryId = mockSrv.categoryId,
                        name = mockSrv.name,
                        description = mockSrv.description,
                        price = mockSrv.price,
                        isActive = true
                    )
                    db.collection("services").document(mockSrv.id).set(service).await()
                }
            }

            // Mock provider seeding removed as requested to keep provider lists fully dynamic
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Provider Side Functions
    suspend fun getProviderProfile(providerId: String): Provider? {
        return try {
            val doc = db.collection("providers").document(providerId).get().await()
            if (doc.exists()) {
                doc.toObject(Provider::class.java)?.copy(uid = doc.id)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveProviderProfile(provider: Provider): Boolean {
        return try {
            db.collection("providers").document(provider.uid).set(provider).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateProviderAvailability(providerId: String, isAvailable: Boolean): Boolean {
        return try {
            db.collection("providers").document(providerId).update("available", isAvailable).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getPendingBookingsForCategories(categoryIds: List<String>): List<Booking> {
        return try {
            val snapshot = db.collection("bookings")
                .whereEqualTo("status", "pending")
                .whereEqualTo("providerId", "")
                .get().await()
            val bookings = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.copy(bookingId = doc.id)
            }.filter { it.categoryId in categoryIds }.sortedByDescending { it.createdAt }

            bookings.map { booking ->
                if (booking.customerPhone.isBlank() || booking.customerName.isBlank()) {
                    try {
                        val userSnapshot = db.collection("users").document(booking.userId).get().await()
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
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getProviderJobs(providerId: String): List<Booking> {
        return try {
            val snapshot = db.collection("bookings")
                .whereEqualTo("providerId", providerId)
                .get().await()
            val bookings = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.copy(bookingId = doc.id)
            }.sortedByDescending { it.createdAt }

            bookings.map { booking ->
                if (booking.customerPhone.isBlank() || booking.customerName.isBlank()) {
                    try {
                        val userSnapshot = db.collection("users").document(booking.userId).get().await()
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
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: String, providerId: String = "", cancelReason: String = ""): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            if (cancelReason.isNotEmpty()) {
                updates["cancelReason"] = cancelReason
            }
            if (providerId.isNotEmpty()) {
                updates["providerId"] = providerId
                try {
                    val providerSnapshot = db.collection("providers").document(providerId).get().await()
                    if (providerSnapshot.exists()) {
                        val name = providerSnapshot.getString("name") ?: ""
                        val phone = providerSnapshot.getString("phone") ?: ""
                        updates["providerName"] = name
                        updates["providerPhone"] = phone
                    } else if (providerId == "test_provider_id_123") {
                        updates["providerName"] = "Test Provider"
                        updates["providerPhone"] = "+92 300 1234567"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (providerId == "test_provider_id_123") {
                        updates["providerName"] = "Test Provider"
                        updates["providerPhone"] = "+92 300 1234567"
                    }
                }
            }
            db.collection("bookings").document(bookingId).update(updates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getAllUsersCount(): Int {
        return try {
            val snapshot = db.collection("users").get().await()
            snapshot.size()
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun getAllProviders(): List<Provider> {
        return try {
            val snapshot = db.collection("providers").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Provider::class.java)?.copy(uid = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAllBookings(): List<Booking> {
        return try {
            val snapshot = db.collection("bookings").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.copy(bookingId = doc.id)
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun updateProviderApprovalStatus(providerId: String, isApproved: Boolean, isRejected: Boolean): Boolean {
        return try {
            val updates = mapOf(
                "approved" to isApproved,
                "rejected" to isRejected
            )
            db.collection("providers").document(providerId).update(updates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun sendChatMessage(bookingId: String, message: ChatMessage): Boolean {
        return try {
            // Restore chat thread visibility for both parties when a new message is sent
            db.collection("bookings").document(bookingId)
                .update("hiddenByUsers", emptyList<String>())
                .await()

            db.collection("bookings")
                .document(bookingId)
                .collection("messages")
                .add(message)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteNotification(notificationId: String): Boolean {
        return try {
            db.collection("notifications").document(notificationId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun clearAllNotifications(recipientId: String): Boolean {
        return try {
            val snapshot = db.collection("notifications")
                .whereEqualTo("recipientId", recipientId)
                .get().await()
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun hideChatThread(bookingId: String, userId: String): Boolean {
        return try {
            db.collection("bookings").document(bookingId)
                .update("hiddenByUsers", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ── Customer Saved Addresses ──────────────────────────────────────────────

    suspend fun getUserAddresses(userId: String): List<com.example.homeserve.data.model.SavedAddress> {
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("addresses")
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(com.example.homeserve.data.model.SavedAddress::class.java)
                    ?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addUserAddress(userId: String, address: com.example.homeserve.data.model.SavedAddress): Boolean {
        return try {
            db.collection("users").document(userId)
                .collection("addresses")
                .document(address.id)
                .set(address).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteUserAddress(userId: String, addressId: String): Boolean {
        return try {
            db.collection("users").document(userId)
                .collection("addresses")
                .document(addressId)
                .delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ── FCM Token Management ──────────────────────────────────────────────────

    suspend fun saveFcmToken(userId: String, token: String, role: String): Boolean {
        return try {
            val collection = if (role == "provider") "providers" else "users"
            db.collection(collection).document(userId)
                .update("fcmToken", token)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteCustomerHistoryAndProfile(userId: String): Boolean {
        return try {
            val batch = db.batch()
            
            // 1. Get all bookings for the customer
            val bookingsSnapshot = db.collection("bookings")
                .whereEqualTo("userId", userId)
                .get().await()
            for (doc in bookingsSnapshot.documents) {
                val bookingId = doc.id
                // Delete all messages under this booking
                val messagesSnapshot = db.collection("bookings").document(bookingId)
                    .collection("messages").get().await()
                for (msgDoc in messagesSnapshot.documents) {
                    db.collection("bookings").document(bookingId)
                        .collection("messages").document(msgDoc.id).delete().await()
                }
                batch.delete(doc.reference)
            }

            // 2. Delete all saved addresses for the customer
            val addressesSnapshot = db.collection("users").document(userId)
                .collection("addresses").get().await()
            for (addrDoc in addressesSnapshot.documents) {
                batch.delete(addrDoc.reference)
            }

            // 3. Delete all notifications for the customer
            val notificationsSnapshot = db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .get().await()
            for (notifDoc in notificationsSnapshot.documents) {
                batch.delete(notifDoc.reference)
            }

            // 4. Delete the customer user document itself
            batch.delete(db.collection("users").document(userId))

            batch.commit().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateUserBlockStatus(userId: String, isBlocked: Boolean): Boolean {
        return try {
            db.collection("users").document(userId).update("blocked", isBlocked).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteProviderHistoryAndProfile(providerId: String): Boolean {
        return try {
            val batch = db.batch()
            
            // 1. Get all bookings assigned to the provider
            val bookingsSnapshot = db.collection("bookings")
                .whereEqualTo("providerId", providerId)
                .get().await()
            for (doc in bookingsSnapshot.documents) {
                val bookingId = doc.id
                // Delete all messages under this booking
                val messagesSnapshot = db.collection("bookings").document(bookingId)
                    .collection("messages").get().await()
                for (msgDoc in messagesSnapshot.documents) {
                    db.collection("bookings").document(bookingId)
                        .collection("messages").document(msgDoc.id).delete().await()
                }
                batch.delete(doc.reference)
            }

            // 2. Delete all notifications for the provider
            val notificationsSnapshot = db.collection("notifications")
                .whereEqualTo("recipientId", providerId)
                .get().await()
            for (notifDoc in notificationsSnapshot.documents) {
                batch.delete(notifDoc.reference)
            }

            // 3. Delete the provider profile document itself
            batch.delete(db.collection("providers").document(providerId))

            batch.commit().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun submitBookingReview(
        bookingId: String,
        rating: Int,
        reviewText: String,
        reviewTags: List<String>
    ): Boolean {
        return try {
            val bookingRef = db.collection("bookings").document(bookingId)
            val bookingDoc = bookingRef.get().await()
            val booking = bookingDoc.toObject(Booking::class.java) ?: return false
            val providerId = booking.providerId

            if (providerId.isBlank()) return false

            val updates = mapOf(
                "rating" to rating,
                "reviewText" to reviewText,
                "reviewTags" to reviewTags,
                "status" to "completed"
            )
            bookingRef.update(updates).await()

            // Calculate new overall rating and reviewCount for the provider
            val bookingsSnapshot = db.collection("bookings")
                .whereEqualTo("providerId", providerId)
                .get()
                .await()

            val providerBookings = bookingsSnapshot.documents.mapNotNull { it.toObject(Booking::class.java) }
            val ratedBookings = providerBookings.filter { it.rating > 0 || (it.bookingId == bookingId && rating > 0) }

            val totalRating = ratedBookings.sumOf { if (it.bookingId == bookingId) rating else it.rating }
            val reviewCount = ratedBookings.size
            val averageRating = if (reviewCount > 0) totalRating.toDouble() / reviewCount else 0.0

            db.collection("providers").document(providerId).update(
                mapOf(
                    "rating" to averageRating,
                    "reviewCount" to reviewCount
                )
            ).await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Updates the paymentStatus field of a completed booking.
     * Called by the provider after collecting payment in cash.
     * @param bookingId  The booking document ID
     * @param status     "paid" or "unpaid"
     */
    suspend fun updatePaymentStatus(bookingId: String, status: String): Boolean {
        return try {
            db.collection("bookings").document(bookingId).update(
                mapOf(
                    "paymentStatus" to status,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Admin Operations for Categories
    suspend fun addCategory(category: Category): Boolean {
        return try {
            db.collection("categories").document(category.categoryId).set(category).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateCategory(category: Category): Boolean {
        return try {
            db.collection("categories").document(category.categoryId).set(category).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteCategory(categoryId: String): Boolean {
        return try {
            db.collection("categories").document(categoryId).delete().await()
            // Cascading delete services under this category
            val services = db.collection("services").whereEqualTo("categoryId", categoryId).get().await()
            for (doc in services.documents) {
                doc.reference.delete().await()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Admin Operations for Sub-Services
    suspend fun addService(service: ServiceModel): Boolean {
        return try {
            db.collection("services").document(service.serviceId).set(service).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateService(service: ServiceModel): Boolean {
        return try {
            db.collection("services").document(service.serviceId).set(service).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteService(serviceId: String): Boolean {
        return try {
            db.collection("services").document(serviceId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Admin Settings
    suspend fun getAdminSettings(adminId: String): com.example.homeserve.data.model.AdminSettings? {
        return try {
            val doc = db.collection("admin_settings").document(adminId).get().await()
            if (doc.exists()) {
                doc.toObject(com.example.homeserve.data.model.AdminSettings::class.java)?.copy(adminId = adminId)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveAdminSettings(settings: com.example.homeserve.data.model.AdminSettings): Boolean {
        return try {
            db.collection("admin_settings").document(settings.adminId).set(settings).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

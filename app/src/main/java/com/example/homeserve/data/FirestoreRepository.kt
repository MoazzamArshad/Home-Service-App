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
            db.collection("bookings").add(booking).await()
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
}

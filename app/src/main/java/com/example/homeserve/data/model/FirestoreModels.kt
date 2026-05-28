package com.example.homeserve.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class User(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "user",
    val address: String = "",
    val password: String = "",
    val profilePhotoUrl: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class Provider(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val categoryId: String = "",
    val bio: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    @get:PropertyName("available") @set:PropertyName("available") @field:PropertyName("available") var isAvailable: Boolean = true,
    @get:PropertyName("approved") @set:PropertyName("approved") @field:PropertyName("approved") var isApproved: Boolean = false,
    @get:PropertyName("rejected") @set:PropertyName("rejected") @field:PropertyName("rejected") var isRejected: Boolean = false,
    val role: String = "provider",
    val radiusKm: Int = 5,
    val providerLatitude: Double = 0.0,
    val providerLongitude: Double = 0.0,
    val address: String = "",
    val profilePhotoUrl: String = "",
    val documentUrl: String = "",
    val idNumber: String = "",
    val password: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class Category(
    @DocumentId val categoryId: String = "",
    val name: String = "",
    val icon: String = "",
    val isActive: Boolean = true
)

data class ServiceModel(
    @DocumentId val serviceId: String = "",
    val categoryId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Int = 0,
    val isActive: Boolean = true
)

data class Booking(
    @DocumentId val bookingId: String = "",
    val userId: String = "",
    val providerId: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val categoryId: String = "",
    val status: String = "pending", // pending, accepted, in_progress, completed, cancelled
    val scheduledDate: Timestamp = Timestamp.now(),
    val totalAmount: Int = 0,
    val paymentStatus: String = "unpaid",
    val address: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val providerName: String = "",
    val providerPhone: String = "",
    val providerPhotoUrl: String = "",
    val customerLatitude: Double = 0.0,
    val customerLongitude: Double = 0.0,
    val cancelReason: String = "",
    val appliedProviderIds: List<String> = emptyList(),
    val applications: List<JobApplication> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class JobApplication(
    val providerId: String = "",
    val providerName: String = "",
    val providerRating: Double = 0.0,
    val bidAmount: Int = 0,
    val pitchNote: String = ""
)

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "", // "customer" or "provider"
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)


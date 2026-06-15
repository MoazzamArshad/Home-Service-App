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
    val fcmToken: String = "",
    @get:PropertyName("blocked") @set:PropertyName("blocked") @field:PropertyName("blocked") var isBlocked: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val bio: String = ""
)

data class Provider(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val categoryId: String = "",
    val selectedServiceIds: String = "",
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
    val fcmToken: String = "",
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
    val geohash: String = "",
    val cancelReason: String = "",
    val appliedProviderIds: List<String> = emptyList(),
    val applications: List<JobApplication> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val hiddenByUsers: List<String> = emptyList(),
    val jobDescription: String = "",
    val problemPhotoUrl: String = "",
    val problemAudioUrl: String = "",
    val customerLastRead: Timestamp? = null,
    val providerLastRead: Timestamp? = null,
    val customerTyping: Boolean = false,
    val providerTyping: Boolean = false,
    val rating: Int = 0,
    val reviewText: String = "",
    val reviewTags: List<String> = emptyList()
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
    val timestamp: Timestamp = Timestamp.now(),
    val voiceUrl: String = "",
    val voiceDuration: Int = 0,
    val imageUrl: String = ""
)

data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "", // userId or providerId
    val title: String = "",
    val message: String = "",
    val read: Boolean = false,
    val targetScreen: String = "", // bookings, provider_jobs, chat/bookingId
    val timestamp: Timestamp = Timestamp.now()
)

data class ChatThread(
    val bookingId: String = "",
    val otherPartyId: String = "",
    val otherPartyName: String = "",
    val otherPartyPhotoUrl: String = "",
    val lastMessageText: String = "Tap to start conversation",
    val lastMessageTimestamp: Timestamp? = null,
    val unreadCount: Int = 0
)

data class SavedAddress(
    val id: String = "",
    val label: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val isDefault: Boolean = false,
    val latitude: Double = 31.5204,
    val longitude: Double = 74.3587
)

data class AdminSettings(
    val adminId: String = "",
    // Notification preferences
    val notifyNewBookings: Boolean = true,
    val notifyProviderRegistrations: Boolean = true,
    val notifySystemAlerts: Boolean = true,
    val notifyEmailEnabled: Boolean = true,
    val notifySmsEnabled: Boolean = false,
    val notifyBookingCancellations: Boolean = true,
    val notifyPaymentUpdates: Boolean = true,
    // Security settings
    val sessionTimeoutMinutes: Int = 30,
    val twoFactorEnabled: Boolean = false,
    val loginNotificationsEnabled: Boolean = true,
    val autoLockEnabled: Boolean = true,
    val requirePasswordChange: Boolean = false,
    val passwordChangeDays: Int = 90,
    val updatedAt: Timestamp = Timestamp.now()
)

data class SupportTicket(
    @DocumentId val id: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val submitterId: String = "",
    val submitterRole: String = "", // "customer" or "provider"
    val subject: String = "",
    val message: String = "",
    val status: String = "open", // open, in-progress, resolved
    val priority: String = "medium", // low, medium, high
    val createdAt: Timestamp = Timestamp.now()
)


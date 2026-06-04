package com.example.homeserve

import android.content.Context
import android.util.Log
import com.example.homeserve.data.FirestoreRepository
import com.example.homeserve.ui.notifications.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FCMService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Called when the FCM token is generated or refreshed.
     * Save it to Firestore so the Cloud Function can target this device.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "New FCM token: $token")
        saveTokenToFirestore(token)
    }

    /**
     * Called when a push notification arrives (even if app is closed).
     * This is the key method that solves the "closed app" problem.
     * Integrates real-time client-side filter choices of the active user.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCMService", "FCM message received: ${message.data}")

        // Extract notification data from the Cloud Function payload
        val title = message.data["title"] ?: message.notification?.title ?: "HomeServe"
        val body = message.data["body"] ?: message.notification?.body ?: ""
        val targetScreen = message.data["targetScreen"] ?: ""

        if (body.isNotBlank()) {
            if (shouldShowNotification(applicationContext, title, body, targetScreen)) {
                NotificationHelper.showNotification(
                    context = applicationContext,
                    title = title,
                    message = body,
                    targetScreen = targetScreen.ifBlank { null }
                )
            } else {
                Log.d("FCMService", "Push notification muted by user preferences: title='$title', body='$body'")
            }
        }
    }

    private fun shouldShowNotification(
        context: Context,
        title: String,
        body: String,
        targetScreen: String
    ): Boolean {
        val sharedPrefs = context.getSharedPreferences("homeserve_prefs", Context.MODE_PRIVATE)
        val notifPrefs = context.getSharedPreferences("homeserve_notifications_prefs", Context.MODE_PRIVATE)
        
        val role = sharedPrefs.getString("saved_role", "customer") ?: "customer"
        val tLower = title.lowercase()
        val bLower = body.lowercase()
        val sLower = targetScreen.lowercase()
        
        if (role == "provider") {
            // 1. Direct Chats
            if (sLower.contains("chat") || tLower.contains("chat") || tLower.contains("message") || bLower.contains("message")) {
                return notifPrefs.getBoolean("notification_pref_provider_chat_alerts", true)
            }
            // 2. Booking Status & Payments
            if (tLower.contains("bid") || bLower.contains("bid") || 
                tLower.contains("accept") || bLower.contains("accept") ||
                tLower.contains("complete") || bLower.contains("complete") ||
                tLower.contains("pay") || bLower.contains("pay") ||
                tLower.contains("cash") || bLower.contains("cash")) {
                return notifPrefs.getBoolean("notification_pref_provider_marketing_alerts", true)
            }
            // 3. New Job Match Alerts
            if (sLower.contains("job") || tLower.contains("job") || bLower.contains("job") ||
                tLower.contains("request") || bLower.contains("request")) {
                return notifPrefs.getBoolean("notification_pref_provider_booking_alerts", true)
            }
            return true
        } else {
            // Customer side
            // 1. Direct Chats
            if (sLower.contains("chat") || tLower.contains("chat") || tLower.contains("message") || bLower.contains("message")) {
                return notifPrefs.getBoolean("notification_pref_customer_chat_alerts", true)
            }
            // 2. Promos & Offers
            if (tLower.contains("promo") || bLower.contains("promo") ||
                tLower.contains("discount") || bLower.contains("discount") ||
                tLower.contains("offer") || bLower.contains("offer")) {
                return notifPrefs.getBoolean("notification_pref_customer_marketing_alerts", false)
            }
            // 3. Booking & Bid updates
            if (sLower.contains("booking") || tLower.contains("booking") || bLower.contains("booking") ||
                tLower.contains("bid") || bLower.contains("bid") ||
                tLower.contains("quote") || bLower.contains("quote")) {
                return notifPrefs.getBoolean("notification_pref_customer_booking_alerts", true)
            }
            return true
        }
    }

    private fun saveTokenToFirestore(token: String) {
        serviceScope.launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser ?: return@launch
                val uid = currentUser.uid
                val db = FirebaseFirestore.getInstance()

                // Check if user is a provider or customer and save token accordingly
                val providerDoc = db.collection("providers").document(uid).get().await()
                if (providerDoc.exists()) {
                    db.collection("providers").document(uid)
                        .update("fcmToken", token)
                        .await()
                    Log.d("FCMService", "FCM token saved for provider $uid")
                }

                val userDoc = db.collection("users").document(uid).get().await()
                if (userDoc.exists()) {
                    db.collection("users").document(uid)
                        .update("fcmToken", token)
                        .await()
                    Log.d("FCMService", "FCM token saved for user $uid")
                }
            } catch (e: Exception) {
                Log.e("FCMService", "Error saving FCM token", e)
            }
        }
    }
}

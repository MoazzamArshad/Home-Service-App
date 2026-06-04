package com.example.homeserve.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationHelper {
    private const val CHANNEL_ID = "homeserve_notifications"
    private const val CHANNEL_NAME = "HomeServe Notifications"
    private const val CHANNEL_DESC = "Real-time updates about your home service bookings"

    private val _navigationRoute = MutableStateFlow<String?>(null)
    val navigationRoute: StateFlow<String?> = _navigationRoute.asStateFlow()

    private val recentlyShown = mutableMapOf<String, Long>()

    fun triggerNavigation(route: String) {
        _navigationRoute.value = route
    }

    fun clearNavigation() {
        _navigationRoute.value = null
    }

    fun showNotification(context: Context, title: String, message: String, targetScreen: String? = null) {
        if (!shouldShowNotification(context, title, message, targetScreen)) {
            return
        }
        val signature = "$title|$message|$targetScreen"
        synchronized(recentlyShown) {
            val currentTime = System.currentTimeMillis()
            val iterator = recentlyShown.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (currentTime - entry.value > 10000) {
                    iterator.remove()
                }
            }
            val lastShown = recentlyShown[signature] ?: 0L
            if (currentTime - lastShown < 3000) {
                return
            }
            recentlyShown[signature] = currentTime
        }
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = CHANNEL_DESC
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                putExtra("target_screen", targetScreen)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // High compatibility built-in icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shouldShowNotification(
        context: Context,
        title: String,
        body: String,
        targetScreen: String?
    ): Boolean {
        try {
            val sharedPrefs = context.getSharedPreferences("homeserve_prefs", Context.MODE_PRIVATE)
            val notifPrefs = context.getSharedPreferences("homeserve_notifications_prefs", Context.MODE_PRIVATE)
            
            val role = sharedPrefs.getString("saved_role", "customer") ?: "customer"
            val tLower = title.lowercase()
            val bLower = body.lowercase()
            val sLower = (targetScreen ?: "").lowercase()
            
            if (role == "provider") {
                // 1. Direct Chats
                if (sLower.contains("chat") || tLower.contains("chat") || tLower.contains("message") || bLower.contains("message")) {
                    return notifPrefs.getBoolean("notification_pref_provider_chat_alerts", true)
                }
                // 2. Booking Status & Payments (like bid confirmations, cancellations, completions, payment confirmation)
                if (tLower.contains("bid") || bLower.contains("bid") || 
                    tLower.contains("accept") || bLower.contains("accept") ||
                    tLower.contains("complete") || bLower.contains("complete") ||
                    tLower.contains("pay") || bLower.contains("pay") ||
                    tLower.contains("cash") || bLower.contains("cash")) {
                    return notifPrefs.getBoolean("notification_pref_provider_marketing_alerts", true)
                }
                // 3. New Job Match Alerts (or cancellations in provider jobs context)
                if (sLower.contains("job") || tLower.contains("job") || bLower.contains("job") ||
                    tLower.contains("request") || bLower.contains("request") ||
                    tLower.contains("cancelled") || bLower.contains("cancelled")) {
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
                    tLower.contains("quote") || bLower.contains("quote") ||
                    tLower.contains("placed") || bLower.contains("placed") ||
                    tLower.contains("started") || bLower.contains("started") ||
                    tLower.contains("completed") || bLower.contains("completed") ||
                    tLower.contains("cancelled") || bLower.contains("cancelled")) {
                    return notifPrefs.getBoolean("notification_pref_customer_booking_alerts", true)
                }
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return true
        }
    }
}

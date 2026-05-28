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
}

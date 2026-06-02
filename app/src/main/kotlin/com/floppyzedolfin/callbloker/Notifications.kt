package com.floppyzedolfin.callbloker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Posts the silent "call blocked" notification. */
object Notifications {

    private const val CHANNEL_ID = "blocked_calls"

    /** Creates the (silent, low-importance) channel if it doesn't exist yet. */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            // IMPORTANCE_LOW shows in the status bar and shade without making a
            // sound; we also clear sound/vibration explicitly to be safe.
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_blocked_calls),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** Shows a status-bar notification that a call from [number] was blocked. */
    // canPost() guards the notify() call below; lint can't follow the indirection.
    @SuppressLint("MissingPermission")
    fun notifyBlocked(context: Context, number: String) {
        if (!canPost(context)) return
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.call_blocked))
            .setContentText(number.ifBlank { context.getString(R.string.unknown_number) })
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .build()
        // One notification per number, so repeat calls update rather than stack.
        NotificationManagerCompat.from(context).notify(number.hashCode(), notification)
    }

    private fun canPost(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

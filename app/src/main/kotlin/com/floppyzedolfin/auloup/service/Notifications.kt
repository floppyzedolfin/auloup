package com.floppyzedolfin.auloup.service

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
import com.floppyzedolfin.auloup.R

/** Posts the silent "calls blocked today" notification. */
object Notifications {

    // v2: bumped to IMPORTANCE_DEFAULT so the wolf shows in the status bar.
    // A channel's importance is immutable once created, so a new id is needed
    // for the change to take effect (harmless — the app isn't released yet).
    private const val CHANNEL_ID = "blocked_calls_v2"

    // A single, stable notification we keep updating with today's running count,
    // rather than one notification per blocked number.
    private const val BLOCKED_TODAY_ID = 1

    /** Creates the (silent, low-importance) channel if it doesn't exist yet. */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            // IMPORTANCE_DEFAULT so the icon appears in the status bar; we clear
            // sound and vibration so it's still silent (no sound, no heads-up
            // beyond the status-bar glyph).
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_blocked_calls),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows/updates one status-bar notification with how many calls were blocked
     * today ([todayCount]) — not the individual numbers.
     */
    // canPost() guards the notify() call below; lint can't follow the indirection.
    @SuppressLint("MissingPermission")
    fun notifyBlockedToday(context: Context, todayCount: Int) {
        if (!canPost(context)) return
        ensureChannel(context)
        val text = context.resources.getQuantityString(R.plurals.calls_blocked_today, todayCount, todayCount)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_wolf)
            // The system header already shows the app name + relative time
            // ("Au loup! · 39m"), so the single content line is just the count.
            .setContentTitle(text)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setShowWhen(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        // Always the same id, so each block updates the count in place.
        NotificationManagerCompat.from(context).notify(BLOCKED_TODAY_ID, notification)
    }

    private fun canPost(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

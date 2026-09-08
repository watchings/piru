package app.piru.android

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    fun schedule(context: Context, atMillis: Long) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            context, 1, Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        if (Build.VERSION.SDK_INT >= 23) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
        } else {
            alarm.set(AlarmManager.RTC_WAKEUP, atMillis, intent)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // The app deliberately does not put substance names or dose content on the lock screen.
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        if (Build.VERSION.SDK_INT < 33 ||
            manager.areNotificationsEnabled()
        ) {
            val notification = androidx.core.app.NotificationCompat.Builder(context, "piru-reminders")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Piru reminder")
                .setContentText("A private journal reminder is ready.")
                .setAutoCancel(true)
                .build()
            manager.notify(1, notification)
        }
    }
}

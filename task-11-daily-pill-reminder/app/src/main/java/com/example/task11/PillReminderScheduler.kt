package com.example.task11

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

class PillReminderScheduler(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun enable() {
        val trigger = computeNext20()
        val pending = reminderPendingIntent()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger,
            pending
        )

        prefs.edit()
            .putBoolean(KEY_ENABLED, true)
            .putLong(KEY_NEXT_TIME, trigger)
            .apply()
    }

    fun disable() {
        alarmManager.cancel(reminderPendingIntent())
        prefs.edit()
            .putBoolean(KEY_ENABLED, false)
            .remove(KEY_NEXT_TIME)
            .apply()
    }

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun getNextTimeMillis(): Long? {
        if (!isEnabled()) return null
        val stored = prefs.getLong(KEY_NEXT_TIME, 0L)
        return if (stored == 0L) null else stored
    }

    fun rescheduleAfterTrigger() {
        if (!isEnabled()) return
        enable()
    }

    private fun computeNext20(): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return next.timeInMillis
    }

    private fun reminderPendingIntent(): PendingIntent {
        val intent = Intent(context, PillReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val PREFS_NAME = "pill_reminder_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_NEXT_TIME = "next_time"
        private const val REQUEST_CODE = 1111
    }
}

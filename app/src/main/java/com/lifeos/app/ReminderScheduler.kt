package com.lifeos.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(reminder: LifeOsReminder) {
        val triggerTime = reminder.scheduledTimeMillis
        val now = System.currentTimeMillis()

        // 1. Notification 5 minutes before
        val fiveMinBefore = triggerTime - 5 * 60 * 1000
        if (fiveMinBefore > now) {
            scheduleAlarm(reminder, fiveMinBefore, "Reminder coming up in 5 minutes")
        }

        // 2. Notification at exact time
        if (triggerTime > now) {
            scheduleAlarm(reminder, triggerTime, "Reminder now")
        }
    }

    private fun scheduleAlarm(reminder: LifeOsReminder, triggerTime: Long, type: String) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("reminder_title", reminder.title)
            putExtra("reminder_type", type)
        }

        val requestCode = (reminder.id + type).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelReminder(reminderId: String) {
        // Need to cancel both alarms
        cancelAlarm(reminderId, "Reminder coming up in 5 minutes")
        cancelAlarm(reminderId, "Reminder now")
    }

    private fun cancelAlarm(reminderId: String, type: String) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val requestCode = (reminderId + type).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAllReminders(userId: String) {
        val localStore = LocalLifeOsStore(context)
        val snapshot = localStore.readSnapshot(
            userId,
            LifeOsSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        )

        snapshot.reminders.forEach { reminder ->
            scheduleReminder(reminder)
        }
    }
}

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("reminder_id") ?: return
        val reminderTitle = intent.getStringExtra("reminder_title") ?: "Reminder"
        val type = intent.getStringExtra("reminder_type") ?: "Reminder"

        val notificationManager = LifeOsNotificationManager(context)
        val reminder = LifeOsReminder(reminderId, reminderTitle, type, 0)
        notificationManager.showReminderNotification(reminder, (reminderId + type).hashCode())
    }
}

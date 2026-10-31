package com.lifeos.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class EventScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleEvent(event: LifeOsEvent) {
        val triggerTime = event.timestamp
        val now = System.currentTimeMillis()
        if (triggerTime <= 0L) return

        val fiveMinBefore = triggerTime - 5 * 60 * 1000
        if (fiveMinBefore > now) {
            scheduleAlarm(event, fiveMinBefore, "Event starts in 5 minutes")
        }
        if (triggerTime > now) {
            scheduleAlarm(event, triggerTime, "Event starts now")
        }
    }

    private fun scheduleAlarm(event: LifeOsEvent, triggerTime: Long, type: String) {
        val intent = Intent(context, EventBroadcastReceiver::class.java).apply {
            putExtra("event_id", event.id)
            putExtra("event_title", event.title)
            putExtra("event_type", type)
        }
        val requestCode = (event.id + type).hashCode()
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

    fun cancelEvent(eventId: String) {
        cancelAlarm(eventId, "Event starts in 5 minutes")
        cancelAlarm(eventId, "Event starts now")
    }

    private fun cancelAlarm(eventId: String, type: String) {
        val intent = Intent(context, EventBroadcastReceiver::class.java)
        val requestCode = (eventId + type).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

class EventBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra("event_id") ?: return
        val eventTitle = intent.getStringExtra("event_title") ?: "Event"
        val type = intent.getStringExtra("event_type") ?: "Event"

        val notificationManager = LifeOsNotificationManager(context)
        val event = LifeOsEvent(eventId, eventTitle, type, "", 0L)
        notificationManager.showEventNotification(event, (eventId + type).hashCode())
    }
}

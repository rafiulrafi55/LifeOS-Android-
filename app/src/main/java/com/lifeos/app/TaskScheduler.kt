package com.lifeos.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class TaskScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTask(task: LifeOsTask) {
        val triggerTime = task.dueTimestamp
        val now = System.currentTimeMillis()
        if (triggerTime <= 0L) return

        val fiveMinBefore = triggerTime - 5 * 60 * 1000
        if (fiveMinBefore > now) {
            scheduleAlarm(task, fiveMinBefore, "Task due in 5 minutes")
        }
        if (triggerTime > now) {
            scheduleAlarm(task, triggerTime, "Task due now")
        }
    }

    private fun scheduleAlarm(task: LifeOsTask, triggerTime: Long, type: String) {
        val intent = Intent(context, TaskBroadcastReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("task_title", task.title)
            putExtra("task_type", type)
        }
        val requestCode = (task.id + type).hashCode()
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

    fun cancelTask(taskId: String) {
        cancelAlarm(taskId, "Task due in 5 minutes")
        cancelAlarm(taskId, "Task due now")
    }

    private fun cancelAlarm(taskId: String, type: String) {
        val intent = Intent(context, TaskBroadcastReceiver::class.java)
        val requestCode = (taskId + type).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

class TaskBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("task_id") ?: return
        val taskTitle = intent.getStringExtra("task_title") ?: "Task"
        val type = intent.getStringExtra("task_type") ?: "Task"

        val notificationManager = LifeOsNotificationManager(context)
        val task = LifeOsTask(taskId, taskTitle, type, false, 0L)
        notificationManager.showTaskDueNotification(task, (taskId + type).hashCode())
    }
}

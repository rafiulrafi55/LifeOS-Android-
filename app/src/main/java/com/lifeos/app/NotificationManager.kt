package com.lifeos.app

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.*

class LifeOsNotificationManager(private val context: Context) {
    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        const val CHANNEL_ID_BUDGET = "budget_alerts"
        const val CHANNEL_ID_TASKS = "task_reminders"
        const val CHANNEL_ID_EVENTS = "event_reminders"
        const val CHANNEL_ID_REMINDERS = "reminders"

        const val NOTIFICATION_ID_BUDGET = 1001
        const val NOTIFICATION_ID_TASK_BASE = 2000
        const val NOTIFICATION_ID_EVENT_BASE = 3000
        const val NOTIFICATION_ID_REMINDER_BASE = 4000
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_BUDGET,
                    "Budget Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Low budget warnings" },

                NotificationChannel(
                    CHANNEL_ID_TASKS,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Reminders for due tasks" },

                NotificationChannel(
                    CHANNEL_ID_EVENTS,
                    "Event Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Reminders for calendar events" },

                NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    "Custom Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Your custom reminders" }
            )

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                // Permission should be requested from the Activity
            }
        }
    }

    fun showBudgetAlert(remainingCapital: Double, currency: LifeOsCurrency) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUDGET)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Low Budget Alert")
            .setContentText("Your remaining capital is ${currency.symbol}${String.format("%.2f", remainingCapital)}. Consider reviewing your expenses.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BUDGET, notification)
    }

    fun showTaskDueNotification(task: LifeOsTask, taskId: Int) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, taskId, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TASKS)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Task Due Today")
            .setContentText("${task.title} - ${task.dueLabel}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_TASK_BASE + taskId, notification)
    }

    fun showEventNotification(event: LifeOsEvent, eventId: Int) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, eventId, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EVENTS)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Event Today")
            .setContentText("${event.title} - ${event.timeLabel}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_EVENT_BASE + eventId, notification)
    }

    fun showReminderNotification(reminder: LifeOsReminder, reminderId: Int) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, reminderId, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Reminder")
            .setContentText("${reminder.title} - ${reminder.scheduleLabel}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_REMINDER_BASE + reminderId, notification)
    }

    fun scheduleMorningEventNotifications(context: Context, userId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, EventMorningBroadcastReceiver::class.java)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}

class EventMorningBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("lifeos_local_store", Context.MODE_PRIVATE)
        // Find user ID from prefs keys
        val allKeys = prefs.all.keys
        val userId = allKeys.firstOrNull { it.startsWith("snapshot_") }?.removePrefix("snapshot_") ?: return

        val localStore = LocalLifeOsStore(context)
        val snapshot = localStore.readSnapshot(userId, LifeOsSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
        val notificationManager = LifeOsNotificationManager(context)

        snapshot.events.forEachIndexed { index, event ->
            notificationManager.showEventNotification(event, index)
        }
    }
}

class BudgetCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()
        val repository = FirebaseLifeOsRepository(applicationContext)
        val notificationManager = LifeOsNotificationManager(applicationContext)
        val currencyStore = CurrencyStore(applicationContext)

        // Get currency and check budget
        val currency = currencyStore.read()
        val localStore = LocalLifeOsStore(applicationContext)
        val snapshot = localStore.readSnapshot(userId, LifeOsSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))

        val totalExpenses = snapshot.expenses.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val totalCapitals = snapshot.capitals.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val remainingCapital = totalCapitals - totalExpenses

        // Convert to BDT equivalent for comparison
        val bdtEquivalent = when (currency.code) {
            "BDT" -> remainingCapital
            "USD" -> remainingCapital * 110  // Approximate conversion
            "EUR" -> remainingCapital * 120
            "GBP" -> remainingCapital * 140
            "JPY" -> remainingCapital * 0.75
            "INR" -> remainingCapital * 1.3
            "KRW" -> remainingCapital * 0.085
            else -> remainingCapital
        }

        if (bdtEquivalent < 100) {
            notificationManager.showBudgetAlert(remainingCapital, currency)
        }

        return Result.success()
    }
}

class TaskEventReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()
        val notificationManager = LifeOsNotificationManager(applicationContext)
        val localStore = LocalLifeOsStore(applicationContext)

        val snapshot = localStore.readSnapshot(userId, LifeOsSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))

        val today = SimpleDateFormat("MMM d", Locale.US).format(Date())

        // Check tasks due today
        snapshot.tasks.filter { !it.completed && it.dueLabel.contains("Today", ignoreCase = true) }
            .forEachIndexed { index, task ->
                notificationManager.showTaskDueNotification(task, index)
            }

        // Check events today
        snapshot.events.forEachIndexed { index, event ->
            notificationManager.showEventNotification(event, index)
        }

        // Check reminders
        snapshot.reminders.forEachIndexed { index, reminder ->
            notificationManager.showReminderNotification(reminder, index)
        }

        return Result.success()
    }
}

fun scheduleNotificationWorkers(context: Context, userId: String) {
    val workManager = WorkManager.getInstance(context)

    // Schedule daily budget check at 9 AM
    val budgetCheckRequest = PeriodicWorkRequestBuilder<BudgetCheckWorker>(1, TimeUnit.DAYS)
        .setInputData(workDataOf("userId" to userId))
        .setInitialDelay(calculateDelayToNextHour(9), TimeUnit.MILLISECONDS)
        .build()

    // Schedule task/event reminders - check every hour during the day
    val taskEventRequest = PeriodicWorkRequestBuilder<TaskEventReminderWorker>(1, TimeUnit.HOURS)
        .setInputData(workDataOf("userId" to userId))
        .build()

    workManager.enqueueUniquePeriodicWork(
        "budget_check",
        ExistingPeriodicWorkPolicy.KEEP,
        budgetCheckRequest
    )

    workManager.enqueueUniquePeriodicWork(
        "task_event_reminders",
        ExistingPeriodicWorkPolicy.KEEP,
        taskEventRequest
    )
}

fun cancelNotificationWorkers(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork("budget_check")
    WorkManager.getInstance(context).cancelUniqueWork("task_event_reminders")
}

private fun calculateDelayToNextHour(targetHour: Int): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, targetHour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        if (before(now)) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    return target.timeInMillis - now.timeInMillis
}

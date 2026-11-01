package com.lifeos.app

import android.content.Context

data class LifeOsProfile(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val goal: String = "",
    val notificationsEnabled: Boolean = true,
    val budgetNotificationsEnabled: Boolean = true,
    val taskNotificationsEnabled: Boolean = true,
    val eventNotificationsEnabled: Boolean = true,
    val reminderNotificationsEnabled: Boolean = true
)

class ProfileStore(context: Context) {
    private val preferences = context.getSharedPreferences("lifeos_profile", Context.MODE_PRIVATE)

    fun read(userId: String): LifeOsProfile = LifeOsProfile(
        firstName = preferences.getString("${userId}_firstName", "") ?: "",
        lastName = preferences.getString("${userId}_lastName", "") ?: "",
        username = preferences.getString("${userId}_username", "") ?: "",
        email = preferences.getString("${userId}_email", "") ?: "",
        goal = preferences.getString("${userId}_goal", "") ?: "",
        notificationsEnabled = preferences.getBoolean("${userId}_notificationsEnabled", true),
        budgetNotificationsEnabled = preferences.getBoolean("${userId}_budgetNotificationsEnabled", true),
        taskNotificationsEnabled = preferences.getBoolean("${userId}_taskNotificationsEnabled", true),
        eventNotificationsEnabled = preferences.getBoolean("${userId}_eventNotificationsEnabled", true),
        reminderNotificationsEnabled = preferences.getBoolean("${userId}_reminderNotificationsEnabled", true)
    )

    fun save(userId: String, profile: LifeOsProfile) {
        preferences.edit()
            .putString("${userId}_firstName", profile.firstName)
            .putString("${userId}_lastName", profile.lastName)
            .putString("${userId}_username", profile.username)
            .putString("${userId}_email", profile.email)
            .putString("${userId}_goal", profile.goal)
            .putBoolean("${userId}_notificationsEnabled", profile.notificationsEnabled)
            .putBoolean("${userId}_budgetNotificationsEnabled", profile.budgetNotificationsEnabled)
            .putBoolean("${userId}_taskNotificationsEnabled", profile.taskNotificationsEnabled)
            .putBoolean("${userId}_eventNotificationsEnabled", profile.eventNotificationsEnabled)
            .putBoolean("${userId}_reminderNotificationsEnabled", profile.reminderNotificationsEnabled)
            .apply()
    }

    fun readDarkMode(userId: String): Boolean = preferences.getBoolean("${userId}_darkMode", false)

    fun saveDarkMode(userId: String, enabled: Boolean) {
        preferences.edit().putBoolean("${userId}_darkMode", enabled).apply()
    }

    fun recordActiveDay(userId: String): Int {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val lastActiveDay = preferences.getString("${userId}_lastActiveDay", null)
        var activeDays = preferences.getInt("${userId}_activeDays", 0)
        if (lastActiveDay != today) {
            activeDays += 1
            preferences.edit()
                .putString("${userId}_lastActiveDay", today)
                .putInt("${userId}_activeDays", activeDays)
                .apply()
        }
        return activeDays
    }
}

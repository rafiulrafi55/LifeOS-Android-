package com.lifeos.app

import android.content.Context

data class LifeOsProfile(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val goal: String = "",
    val notificationsEnabled: Boolean = true
)

class ProfileStore(context: Context) {
    private val preferences = context.getSharedPreferences("lifeos_profile", Context.MODE_PRIVATE)

    fun read(userId: String): LifeOsProfile = LifeOsProfile(
        firstName = preferences.getString("${userId}_firstName", "") ?: "",
        lastName = preferences.getString("${userId}_lastName", "") ?: "",
        username = preferences.getString("${userId}_username", "") ?: "",
        email = preferences.getString("${userId}_email", "") ?: "",
        goal = preferences.getString("${userId}_goal", "") ?: "",
        notificationsEnabled = preferences.getBoolean("${userId}_notificationsEnabled", true)
    )

    fun save(userId: String, profile: LifeOsProfile) {
        preferences.edit()
            .putString("${userId}_firstName", profile.firstName)
            .putString("${userId}_lastName", profile.lastName)
            .putString("${userId}_username", profile.username)
            .putString("${userId}_email", profile.email)
            .putString("${userId}_goal", profile.goal)
            .putBoolean("${userId}_notificationsEnabled", profile.notificationsEnabled)
            .apply()
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

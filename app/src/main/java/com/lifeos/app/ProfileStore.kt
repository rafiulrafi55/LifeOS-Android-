package com.lifeos.app

import android.content.Context

data class LifeOsProfile(
    val firstName: String = "Alex",
    val lastName: String = "Morgan",
    val username: String = "alexmorgan",
    val email: String = "alex@example.com",
    val goal: String = "Create more calm and focus",
    val notificationsEnabled: Boolean = true
)

class ProfileStore(context: Context) {
    private val preferences = context.getSharedPreferences("lifeos_profile", Context.MODE_PRIVATE)

    fun read(): LifeOsProfile = LifeOsProfile(
        firstName = preferences.getString("firstName", "Alex") ?: "Alex",
        lastName = preferences.getString("lastName", "Morgan") ?: "Morgan",
        username = preferences.getString("username", "alexmorgan") ?: "alexmorgan",
        email = preferences.getString("email", "alex@example.com") ?: "alex@example.com",
        goal = preferences.getString("goal", "Create more calm and focus") ?: "Create more calm and focus",
        notificationsEnabled = preferences.getBoolean("notificationsEnabled", true)
    )

    fun save(profile: LifeOsProfile) {
        preferences.edit()
            .putString("firstName", profile.firstName)
            .putString("lastName", profile.lastName)
            .putString("username", profile.username)
            .putString("email", profile.email)
            .putString("goal", profile.goal)
            .putBoolean("notificationsEnabled", profile.notificationsEnabled)
            .apply()
    }
}

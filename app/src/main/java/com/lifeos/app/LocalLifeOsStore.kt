package com.lifeos.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LocalLifeOsStore(context: Context) {
    private val preferences = context.getSharedPreferences("lifeos_local_store", Context.MODE_PRIVATE)

    fun readSnapshot(userId: String, fallback: LifeOsSnapshot): LifeOsSnapshot {
        val raw = preferences.getString(snapshotKey(userId), null) ?: return fallback
        return runCatching { decodeSnapshot(JSONObject(raw), fallback) }.getOrDefault(fallback)
    }

    fun saveSnapshot(userId: String, snapshot: LifeOsSnapshot) {
        preferences.edit().putString(snapshotKey(userId), encodeSnapshot(snapshot).toString()).apply()
    }

    private fun encodeSnapshot(snapshot: LifeOsSnapshot): JSONObject = JSONObject().apply {
        put("tasks", JSONArray().apply { snapshot.tasks.forEach { put(JSONObject().apply { put("id", it.id); put("title", it.title); put("dueLabel", it.dueLabel); put("completed", it.completed) }) } })
        put("events", JSONArray().apply { snapshot.events.forEach { put(JSONObject().apply { put("id", it.id); put("title", it.title); put("timeLabel", it.timeLabel); put("colorKey", it.colorKey) }) } })
        put("notes", JSONArray().apply { snapshot.notes.forEach { put(JSONObject().apply { put("id", it.id); put("title", it.title); put("preview", it.preview); put("updatedLabel", it.updatedLabel) }) } })
        put("expenses", JSONArray().apply { snapshot.expenses.forEach { put(JSONObject().apply { put("id", it.id); put("merchant", it.merchant); put("category", it.category); put("amount", it.amount) }) } })
        put("reminders", JSONArray().apply { snapshot.reminders.forEach { put(JSONObject().apply { put("id", it.id); put("title", it.title); put("scheduleLabel", it.scheduleLabel) }) } })
    }

    private fun decodeSnapshot(json: JSONObject, fallback: LifeOsSnapshot): LifeOsSnapshot = LifeOsSnapshot(
        tasks = json.array("tasks", fallback.tasks) { item -> LifeOsTask(item.string("id"), item.string("title"), item.string("dueLabel"), item.optBoolean("completed")) },
        events = json.array("events", fallback.events) { item -> LifeOsEvent(item.string("id"), item.string("title"), item.string("timeLabel"), item.string("colorKey")) },
        notes = json.array("notes", fallback.notes) { item -> LifeOsNote(item.string("id"), item.string("title"), item.string("preview"), item.string("updatedLabel")) },
        expenses = json.array("expenses", fallback.expenses) { item -> LifeOsExpense(item.string("id"), item.string("merchant"), item.string("category"), item.string("amount")) },
        reminders = json.array("reminders", fallback.reminders) { item -> LifeOsReminder(item.string("id"), item.string("title"), item.string("scheduleLabel")) }
    )

    private fun <T> JSONObject.array(key: String, fallback: List<T>, map: (JSONObject) -> T): List<T> {
        val array = optJSONArray(key) ?: return fallback
        return buildList { for (index in 0 until array.length()) add(map(array.getJSONObject(index))) }
    }

    private fun JSONObject.string(key: String): String = optString(key, "")

    private fun snapshotKey(userId: String): String = "$KEY_SNAPSHOT_PREFIX$userId"

    private companion object {
        const val KEY_SNAPSHOT_PREFIX = "snapshot_"
    }
}

package com.lifeos.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LocalLifeOsStore(context: Context) {
    private val preferences = context.getSharedPreferences("lifeos_local_store", Context.MODE_PRIVATE)

    fun readSnapshot(userId: String, fallback: LifeOsSnapshot): LifeOsSnapshot {
        val raw = preferences.getString(snapshotKey(userId), null) ?: return fallback
        return runCatching { decodeSnapshot(JSONObject(raw), fallback).removeLegacyDemoRecords() }.getOrDefault(fallback)
    }

    fun saveSnapshot(userId: String, snapshot: LifeOsSnapshot) {
        preferences.edit().putString(snapshotKey(userId), encodeSnapshot(snapshot).toString()).apply()
    }

    private fun encodeSnapshot(snapshot: LifeOsSnapshot): JSONObject = JSONObject().apply {
        put("tasks", JSONArray().apply {
            snapshot.tasks.forEach { task ->
                val taskObj = JSONObject()
                taskObj.put("id", task.id)
                taskObj.put("title", task.title)
                taskObj.put("dueLabel", task.dueLabel)
                taskObj.put("completed", task.completed)
                taskObj.put("dueTimestamp", task.dueTimestamp)
                put(taskObj)
            }
        })
        put("events", JSONArray().apply {
            snapshot.events.forEach { event ->
                val eventObj = JSONObject()
                eventObj.put("id", event.id)
                eventObj.put("title", event.title)
                eventObj.put("timeLabel", event.timeLabel)
                eventObj.put("colorKey", event.colorKey)
                eventObj.put("timestamp", event.timestamp)
                put(eventObj)
            }
        })
        put("notes", JSONArray().apply {
            snapshot.notes.forEach { note ->
                val noteObj = JSONObject()
                noteObj.put("id", note.id)
                noteObj.put("title", note.title)
                noteObj.put("preview", note.preview)
                noteObj.put("updatedLabel", note.updatedLabel)
                put(noteObj)
            }
        })
        put("expenses", JSONArray().apply {
            snapshot.expenses.forEach { expense ->
                val expenseObj = JSONObject()
                expenseObj.put("id", expense.id)
                expenseObj.put("merchant", expense.merchant)
                expenseObj.put("category", expense.category)
                expenseObj.put("amount", expense.amount)
                expenseObj.put("timestamp", expense.timestamp)
                put(expenseObj)
            }
        })
        put("reminders", JSONArray().apply {
            snapshot.reminders.forEach { reminder ->
                val reminderObj = JSONObject()
                reminderObj.put("id", reminder.id)
                reminderObj.put("title", reminder.title)
                reminderObj.put("scheduleLabel", reminder.scheduleLabel)
                reminderObj.put("scheduledTimeMillis", reminder.scheduledTimeMillis)
                put(reminderObj)
            }
        })
        put("capitals", JSONArray().apply {
            snapshot.capitals.forEach { capital ->
                val capitalObj = JSONObject()
                capitalObj.put("id", capital.id)
                capitalObj.put("source", capital.source)
                capitalObj.put("amount", capital.amount)
                capitalObj.put("timestamp", capital.timestamp)
                put(capitalObj)
            }
        })
    }

    private fun decodeSnapshot(json: JSONObject, fallback: LifeOsSnapshot): LifeOsSnapshot = LifeOsSnapshot(
        tasks = json.array("tasks", fallback.tasks) { item -> LifeOsTask(item.string("id"), item.string("title"), item.string("dueLabel"), item.optBoolean("completed"), item.optLong("dueTimestamp", 0L)) },
        events = json.array("events", fallback.events) { item -> LifeOsEvent(item.string("id"), item.string("title"), item.string("timeLabel"), item.string("colorKey"), item.optLong("timestamp", 0L)) },
        notes = json.array("notes", fallback.notes) { item -> LifeOsNote(item.string("id"), item.string("title"), item.string("preview"), item.string("updatedLabel")) },
        expenses = json.array("expenses", fallback.expenses) { item -> LifeOsExpense(item.string("id"), item.string("merchant"), item.string("category"), item.string("amount"), item.optLong("timestamp", System.currentTimeMillis())) },
        reminders = json.array("reminders", fallback.reminders) { item -> LifeOsReminder(item.string("id"), item.string("title"), item.string("scheduleLabel"), item.optLong("scheduledTimeMillis", 0)) },
        capitals = json.optJSONArray("capitals")?.let { array ->
            buildList { for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(LifeOsCapital(item.optString("id", ""), item.optString("source", ""), item.optString("amount", ""), item.optLong("timestamp", System.currentTimeMillis())))
            }}
        } ?: emptyList()
    )

    private fun <T> JSONObject.array(key: String, fallback: List<T>, map: (JSONObject) -> T): List<T> {
        val array = optJSONArray(key) ?: return fallback
        return buildList { for (index in 0 until array.length()) add(map(array.getJSONObject(index))) }
    }

    private fun JSONObject.string(key: String): String = optString(key, "")

    private fun LifeOsSnapshot.removeLegacyDemoRecords(): LifeOsSnapshot = copy(
        tasks = tasks.filterNot { it.id in setOf("task-1", "task-2", "task-3") },
        events = events.filterNot { it.id in setOf("event-1", "event-2") },
        notes = notes.filterNot { it.id in setOf("note-1", "note-2") },
        reminders = reminders.filterNot { it.id in setOf("reminder-1", "reminder-2") }
    )

    private fun snapshotKey(userId: String): String = "$KEY_SNAPSHOT_PREFIX$userId"

    private companion object {
        const val KEY_SNAPSHOT_PREFIX = "snapshot_"
    }
}

package com.lifeos.app

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class LifeOsTask(
    val id: String,
    val title: String,
    val dueLabel: String,
    val completed: Boolean = false
)

data class LifeOsEvent(
    val id: String,
    val title: String,
    val timeLabel: String,
    val colorKey: String
)

data class LifeOsNote(
    val id: String,
    val title: String,
    val preview: String,
    val updatedLabel: String
)

data class LifeOsExpense(
    val id: String,
    val merchant: String,
    val category: String,
    val amount: String
)

data class LifeOsReminder(
    val id: String,
    val title: String,
    val scheduleLabel: String
)

data class LifeOsSnapshot(
    val tasks: List<LifeOsTask>,
    val events: List<LifeOsEvent>,
    val notes: List<LifeOsNote>,
    val expenses: List<LifeOsExpense>,
    val reminders: List<LifeOsReminder>
)

interface LifeOsRepository {
    fun observeSnapshot(): Flow<LifeOsSnapshot>
    fun toggleTask(taskId: String)
    fun updateSnapshot(transform: (LifeOsSnapshot) -> LifeOsSnapshot)
    fun syncToFirebase(callback: (Result<Unit>) -> Unit)
    fun restoreFromFirebase(callback: (Result<Unit>) -> Unit)
}

class InMemoryLifeOsRepository : LifeOsRepository {
    private val snapshot = MutableStateFlow(
        LifeOsSnapshot(
            tasks = emptyList(),
            events = emptyList(),
            notes = emptyList(),
            expenses = listOf(
                
            ),
            reminders = emptyList()
        )
    )

    override fun observeSnapshot(): Flow<LifeOsSnapshot> = snapshot.asStateFlow()

    override fun toggleTask(taskId: String) {
        snapshot.value = snapshot.value.copy(
            tasks = snapshot.value.tasks.map { task ->
                if (task.id == taskId) task.copy(completed = !task.completed) else task
            }
        )
    }

    override fun updateSnapshot(transform: (LifeOsSnapshot) -> LifeOsSnapshot) {
        snapshot.value = transform(snapshot.value)
    }

    override fun syncToFirebase(callback: (Result<Unit>) -> Unit) = callback(Result.success(Unit))

    override fun restoreFromFirebase(callback: (Result<Unit>) -> Unit) = callback(Result.failure(IllegalStateException("Firebase restore is only available for a signed-in account.")))
}

class FirebaseLifeOsRepository(context: Context) : LifeOsRepository {
    private val fallback = InMemoryLifeOsRepository()
    private val localStore = LocalLifeOsStore(context)
    private val profileStore = ProfileStore(context)
    private val snapshot = MutableStateFlow(fallbackSnapshot())
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    init {
        auth.addAuthStateListener { authState ->
            val userId = authState.currentUser?.uid
            if (userId != null) {
                snapshot.value = localStore.readSnapshot(userId, fallbackSnapshot())
                observeTasks(userId)
            } else {
                snapshot.value = fallbackSnapshot()
            }
        }
    }

    override fun observeSnapshot(): Flow<LifeOsSnapshot> = snapshot.asStateFlow()

    override fun toggleTask(taskId: String) {
        val task = snapshot.value.tasks.firstOrNull { it.id == taskId } ?: return
        val updated = snapshot.value.copy(tasks = snapshot.value.tasks.map {
            if (it.id == taskId) it.copy(completed = !it.completed) else it
        })
        snapshot.value = updated
        auth.currentUser?.uid?.let { userId ->
            localStore.saveSnapshot(userId, updated)
            firestore.collection("users").document(userId).collection("tasks").document(taskId)
                .set(mapOf("title" to task.title, "dueLabel" to task.dueLabel, "completed" to !task.completed))
        }
    }

    override fun updateSnapshot(transform: (LifeOsSnapshot) -> LifeOsSnapshot) {
        val updated = transform(snapshot.value)
        snapshot.value = updated
        auth.currentUser?.uid?.let { localStore.saveSnapshot(it, updated) }
    }

    override fun syncToFirebase(callback: (Result<Unit>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(Result.failure(IllegalStateException("Sign in before syncing to Firebase.")))
            return
        }

        val userDocument = firestore.collection("users").document(userId)
        val batch = firestore.batch()
        val profile = profileStore.read(userId)
        batch.set(userDocument, mapOf(
            "firstName" to profile.firstName,
            "lastName" to profile.lastName,
            "username" to profile.username,
            "email" to profile.email,
            "goal" to profile.goal,
            "notificationsEnabled" to profile.notificationsEnabled,
            "lastSyncedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        ), com.google.firebase.firestore.SetOptions.merge())
        syncCollection(batch, userDocument, "tasks", snapshot.value.tasks.map { it.id to mapOf("title" to it.title, "dueLabel" to it.dueLabel, "completed" to it.completed) })
        syncCollection(batch, userDocument, "events", snapshot.value.events.map { it.id to mapOf("title" to it.title, "timeLabel" to it.timeLabel, "colorKey" to it.colorKey) })
        syncCollection(batch, userDocument, "notes", snapshot.value.notes.map { it.id to mapOf("title" to it.title, "preview" to it.preview, "updatedLabel" to it.updatedLabel) })
        syncCollection(batch, userDocument, "expenses", snapshot.value.expenses.map { it.id to mapOf("merchant" to it.merchant, "category" to it.category, "amount" to it.amount) })
        syncCollection(batch, userDocument, "reminders", snapshot.value.reminders.map { it.id to mapOf("title" to it.title, "scheduleLabel" to it.scheduleLabel) })
        batch.set(userDocument.collection("snapshots").document("latest"), snapshotDocument(snapshot.value))
        batch.commit().addOnSuccessListener { callback(Result.success(Unit)) }.addOnFailureListener { callback(Result.failure(it)) }
    }

    override fun restoreFromFirebase(callback: (Result<Unit>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(Result.failure(IllegalStateException("Sign in before restoring from Firebase.")))
            return
        }
        firestore.collection("users").document(userId).collection("snapshots").document("latest").get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    callback(Result.failure(IllegalStateException("No Firebase backup was found.")))
                    return@addOnSuccessListener
                }
                val restored = snapshotFromDocument(document.data.orEmpty())
                snapshot.value = restored
                localStore.saveSnapshot(userId, restored)
                callback(Result.success(Unit))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun snapshotDocument(snapshot: LifeOsSnapshot): Map<String, Any> = mapOf(
        "tasks" to snapshot.tasks.map { mapOf("id" to it.id, "title" to it.title, "dueLabel" to it.dueLabel, "completed" to it.completed) },
        "events" to snapshot.events.map { mapOf("id" to it.id, "title" to it.title, "timeLabel" to it.timeLabel, "colorKey" to it.colorKey) },
        "notes" to snapshot.notes.map { mapOf("id" to it.id, "title" to it.title, "preview" to it.preview, "updatedLabel" to it.updatedLabel) },
        "expenses" to snapshot.expenses.map { mapOf("id" to it.id, "merchant" to it.merchant, "category" to it.category, "amount" to it.amount) },
        "reminders" to snapshot.reminders.map { mapOf("id" to it.id, "title" to it.title, "scheduleLabel" to it.scheduleLabel) }
    )

    private fun snapshotFromDocument(data: Map<String, Any>): LifeOsSnapshot = LifeOsSnapshot(
        tasks = (data["tasks"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.let { item -> LifeOsTask(item["id"].toString(), item["title"].toString(), item["dueLabel"].toString(), item["completed"] as? Boolean ?: false) } },
        events = (data["events"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.let { item -> LifeOsEvent(item["id"].toString(), item["title"].toString(), item["timeLabel"].toString(), item["colorKey"].toString()) } },
        notes = (data["notes"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.let { item -> LifeOsNote(item["id"].toString(), item["title"].toString(), item["preview"].toString(), item["updatedLabel"].toString()) } },
        expenses = (data["expenses"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.let { item -> LifeOsExpense(item["id"].toString(), item["merchant"].toString(), item["category"].toString(), item["amount"].toString()) } },
        reminders = (data["reminders"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.let { item -> LifeOsReminder(item["id"].toString(), item["title"].toString(), item["scheduleLabel"].toString()) } }
    )

    private fun syncCollection(
        batch: com.google.firebase.firestore.WriteBatch,
        userDocument: com.google.firebase.firestore.DocumentReference,
        collection: String,
        records: List<Pair<String, Map<String, Any>>>
    ) {
        records.forEach { (recordId, record) ->
            batch.set(userDocument.collection(collection).document(recordId), record)
        }
    }

    private fun observeTasks(userId: String) {
        firestore.collection("users").document(userId).collection("tasks")
            .addSnapshotListener { result, _ ->
                if (result == null || result.isEmpty) return@addSnapshotListener
                val tasks = result.documents.mapNotNull { document ->
                    document.toObject(FirestoreTask::class.java)?.let { task ->
                        LifeOsTask(document.id, task.title, task.dueLabel, task.completed)
                    }
                }
                val updated = snapshot.value.copy(tasks = tasks)
                snapshot.value = updated
                localStore.saveSnapshot(userId, updated)
            }
    }

    private data class FirestoreTask(
        val title: String = "",
        val dueLabel: String = "",
        val completed: Boolean = false
    )

    private fun fallbackSnapshot(): LifeOsSnapshot = fallback.observeSnapshotValue()
}

private fun InMemoryLifeOsRepository.observeSnapshotValue(): LifeOsSnapshot =
    LifeOsSnapshot(
        tasks = emptyList(),
        events = emptyList(),
        notes = emptyList(),
        expenses = listOf(
            
        ),
        reminders = emptyList()
    )

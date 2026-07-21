package com.vincentwang.copilot.data

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Local persistence via Room plus best-effort mirroring to Cloud Firestore.
 *
 * This mirrors the iOS setup where NSPersistentCloudKitContainer keeps a
 * local Core Data store in sync with the user's private CloudKit database:
 * every mutation lands in Room first (offline-first), and when Firebase is
 * configured and a user is signed in it is mirrored to
 * users/{uid}/items/{itemId}. A snapshot listener plays the role of the
 * NSPersistentStoreRemoteChange notification, folding remote changes back
 * into Room.
 */
class ItemRepository private constructor(private val app: Application) {

    private val dao = CopilotDatabase.get(app).itemDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listener: ListenerRegistration? = null

    val items: Flow<List<Item>> = dao.observeAll()

    private val cloudAvailable: Boolean
        get() = FirebaseApp.getApps(app).isNotEmpty()

    private fun remoteItems() = FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
        FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("items")
    }

    fun startRemoteSync() {
        if (!cloudAvailable) return
        val collection = remoteItems() ?: return
        listener?.remove()
        listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            // Only fold in server-originated changes; local writes are
            // already in Room.
            if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
            scope.launch {
                val upserts = mutableListOf<Item>()
                for (change in snapshot.documentChanges) {
                    when (change.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                            Item.fromMap(change.document.data)?.let(upserts::add)
                        DocumentChange.Type.REMOVED ->
                            dao.deleteById(change.document.id)
                    }
                }
                if (upserts.isNotEmpty()) dao.upsertAll(upserts)
            }
        }
    }

    fun stopRemoteSync() {
        listener?.remove()
        listener = null
    }

    suspend fun insert(items: List<Item>) {
        dao.upsertAll(items)
        val collection = remoteItems() ?: return
        runCatching {
            val batch = FirebaseFirestore.getInstance().batch()
            items.forEach { batch.set(collection.document(it.id), it.toMap()) }
            batch.commit().await()
        }
    }

    suspend fun deleteAll() {
        dao.deleteAll()
        val collection = remoteItems() ?: return
        runCatching {
            val docs = collection.get().await()
            val batch = FirebaseFirestore.getInstance().batch()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    companion object {
        @Volatile
        private var instance: ItemRepository? = null

        fun get(app: Application): ItemRepository =
            instance ?: synchronized(this) {
                instance ?: ItemRepository(app).also { instance = it }
            }
    }
}

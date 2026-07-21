package com.vincentwang.copilot.data

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
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
 * users/{uid}/items/{itemId} (and users/{uid}/aircraft/{id} for saved
 * aircraft profiles). Snapshot listeners play the role of the
 * NSPersistentStoreRemoteChange notification, folding remote changes back
 * into Room.
 */
class ItemRepository private constructor(private val app: Application) {

    private val itemDao = CopilotDatabase.get(app).itemDao()
    private val aircraftDao = CopilotDatabase.get(app).aircraftDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listeners = mutableListOf<ListenerRegistration>()

    val items: Flow<List<Item>> = itemDao.observeAll()
    val aircraft: Flow<List<AircraftProfile>> = aircraftDao.observeAll()

    private val cloudAvailable: Boolean
        get() = FirebaseApp.getApps(app).isNotEmpty()

    private fun userCollection(name: String): CollectionReference? =
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection(name)
        }

    private fun remoteItems() = userCollection("items")
    private fun remoteAircraft() = userCollection("aircraft")

    fun startRemoteSync() {
        if (!cloudAvailable) return
        stopRemoteSync()
        remoteItems()?.let { collection ->
            listeners += collection.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
                scope.launch {
                    val upserts = mutableListOf<Item>()
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                                Item.fromMap(change.document.data)?.let(upserts::add)
                            DocumentChange.Type.REMOVED ->
                                itemDao.deleteById(change.document.id)
                        }
                    }
                    if (upserts.isNotEmpty()) itemDao.upsertAll(upserts)
                }
            }
        }
        remoteAircraft()?.let { collection ->
            listeners += collection.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
                scope.launch {
                    val upserts = mutableListOf<AircraftProfile>()
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                                AircraftProfile.fromMap(change.document.data)?.let(upserts::add)
                            DocumentChange.Type.REMOVED ->
                                aircraftDao.deleteById(change.document.id)
                        }
                    }
                    if (upserts.isNotEmpty()) aircraftDao.upsertAll(upserts)
                }
            }
        }
    }

    fun stopRemoteSync() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    // MARK: Logbook entries

    suspend fun upsert(items: List<Item>) {
        itemDao.upsertAll(items)
        val collection = remoteItems() ?: return
        runCatching {
            val batch = FirebaseFirestore.getInstance().batch()
            items.forEach { batch.set(collection.document(it.id), it.toMap()) }
            batch.commit().await()
        }
    }

    suspend fun delete(item: Item) {
        itemDao.deleteById(item.id)
        val collection = remoteItems() ?: return
        runCatching { collection.document(item.id).delete().await() }
    }

    suspend fun deleteAllItems() {
        itemDao.deleteAll()
        val collection = remoteItems() ?: return
        runCatching {
            val docs = collection.get().await()
            val batch = FirebaseFirestore.getInstance().batch()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    // MARK: Saved aircraft profiles

    suspend fun upsertAircraft(profile: AircraftProfile) {
        aircraftDao.upsertAll(listOf(profile))
        val collection = remoteAircraft() ?: return
        runCatching { collection.document(profile.id).set(profile.toMap()).await() }
    }

    suspend fun deleteAircraft(profile: AircraftProfile) {
        aircraftDao.deleteById(profile.id)
        val collection = remoteAircraft() ?: return
        runCatching { collection.document(profile.id).delete().await() }
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

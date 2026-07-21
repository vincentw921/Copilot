package com.vincentwang.copilot.features.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vincentwang.copilot.CopilotApp
import com.vincentwang.copilot.data.Item
import com.vincentwang.copilot.data.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

// Android counterpart of HomeModel.swift.
class HomeModel(app: Application) : AndroidViewModel(app) {

    sealed interface Status {
        data object Idle : Status
        data object Loading : Status
        data class Success(val id: String) : Status
        data object NotAvailable : Status
        data class Failure(val message: String) : Status
    }

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status

    private val repository = ItemRepository.get(app)

    // Room Flow plays the role of the SwiftUI @FetchRequest.
    val items: StateFlow<List<Item>> = repository.items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Equivalent of listening for NSPersistentStoreRemoteChange: fold
        // remote Firestore changes back into the local store.
        repository.startRemoteSync()
    }

    fun fetchCloudID() {
        _status.value = Status.Loading

        if (!CopilotApp.isCloudConfigured(getApplication())) {
            _status.value = Status.NotAvailable
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _status.value = Status.NotAvailable
            return
        }
        // Prefer the account's display name (the iOS app asks CloudKit for
        // the discoverable user identity), falling back to the UID.
        val display = user.displayName?.takeIf { it.isNotBlank() } ?: user.uid
        _status.value = Status.Success(display)
    }

    fun insertTestData(count: Int = 5) {
        viewModelScope.launch {
            try {
                val now = Calendar.getInstance()
                val samples = (0 until count).map { i ->
                    val date = (now.clone() as Calendar)
                        .apply { add(Calendar.DAY_OF_YEAR, -i) }
                    val totalTime = 0.6 + Math.random() * (2.2 - 0.6)
                    Item(
                        id = UUID.randomUUID().toString(),
                        date = date.timeInMillis,
                        aircraftType = "C172",
                        aircraftRegistration = "N${(1000..9999).random()}${listOf("AB", "CD", "EF").random()}",
                        departureAirport = "KSNA",
                        arrivalAirport = listOf("KLAX", "KSQL", "KPAO", "KSBP").random(),
                        totalTime = totalTime,
                        picTime = totalTime,
                        dayTakeoffs = (1..3).random(),
                        dayFullStopLandings = (1..3).random()
                    )
                }
                repository.insert(samples)
            } catch (e: Exception) {
                _status.value = Status.Failure("Seed failed: ${e.localizedMessage}")
            }
        }
    }

    fun deleteAllItems() {
        viewModelScope.launch {
            try {
                repository.deleteAll()
            } catch (e: Exception) {
                _status.value = Status.Failure(e.localizedMessage ?: "Delete failed.")
            }
        }
    }

    override fun onCleared() {
        repository.stopRemoteSync()
    }
}

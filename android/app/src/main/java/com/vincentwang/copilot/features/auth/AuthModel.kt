package com.vincentwang.copilot.features.auth

import android.app.Application
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.vincentwang.copilot.CopilotApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Android counterpart of AuthModel.swift.
 *
 * On iOS the "account" is the device's iCloud account; the closest Android
 * equivalent is the user's Google account, surfaced through Firebase Auth
 * with Google Sign-In. The signed-in Firebase UID plays the role of the
 * CloudKit user record ID and is cached in the encrypted SecureStore
 * (Keychain equivalent) for a fast path on launch.
 */
class AuthModel(app: Application) : AndroidViewModel(app) {

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data class SignedIn(val cloudID: String) : State
        data object SignedOut : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private val secureStore = SecureStore(app)

    private val authListener = FirebaseAuth.AuthStateListener {
        // Equivalent of observing CKAccountChanged on iOS.
        viewModelScope.launch { refresh() }
    }

    init {
        if (cloudConfigured) {
            FirebaseAuth.getInstance().addAuthStateListener(authListener)
        }
    }

    private val cloudConfigured: Boolean
        get() = CopilotApp.isCloudConfigured(getApplication())

    fun start() {
        // Fast-path from cache, like the Keychain lookup on iOS.
        val cached = secureStore.cloudID
        if (cached != null) {
            _state.value = State.SignedIn(cloudID = cached)
        } else {
            viewModelScope.launch { refresh() }
        }
    }

    suspend fun refresh() {
        _state.value = State.Checking

        if (!cloudConfigured) {
            secureStore.cloudID = null
            _state.value = State.SignedOut
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            secureStore.cloudID = null
            _state.value = State.SignedOut
        } else {
            secureStore.cloudID = user.uid
            _state.value = State.SignedIn(cloudID = user.uid)
        }
    }

    /** Interactive Google Sign-In via Credential Manager. Needs an Activity context. */
    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            _state.value = State.Checking
            try {
                if (!cloudConfigured) {
                    _state.value = State.Error(
                        "Firebase is not configured. Add google-services.json to android/app/."
                    )
                    return@launch
                }
                val app = getApplication<Application>()
                val webClientIdRes = app.resources.getIdentifier(
                    "default_web_client_id", "string", app.packageName
                )
                if (webClientIdRes == 0) {
                    _state.value = State.Error(
                        "Missing default_web_client_id. Enable Google Sign-In in the Firebase console."
                    )
                    return@launch
                }

                val option = GetGoogleIdOption.Builder()
                    .setServerClientId(app.getString(webClientIdRes))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()

                val result = CredentialManager.create(activityContext)
                    .getCredential(activityContext, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential =
                        GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                    refresh()
                } else {
                    _state.value = State.Error("Unexpected credential type.")
                }
            } catch (e: Exception) {
                _state.value = State.Error(e.localizedMessage ?: "Sign-in failed.")
            }
        }
    }

    fun signOutLocally() {
        // Like iOS: clear the local session and cache. (Firebase Auth can
        // actually sign out, unlike iCloud, so do that too.)
        if (cloudConfigured) FirebaseAuth.getInstance().signOut()
        secureStore.cloudID = null
        _state.value = State.SignedOut
    }

    override fun onCleared() {
        if (cloudConfigured) {
            FirebaseAuth.getInstance().removeAuthStateListener(authListener)
        }
    }
}

package com.vincentwang.copilot.features.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Android counterpart of Keychain.swift: a Keystore-backed encrypted store
// holding the cached cloud user ID.
class SecureStore(context: Context) {

    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "copilot_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var cloudID: String?
        get() = prefs.getString(KEY_CLOUD_ID, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_CLOUD_ID) else putString(KEY_CLOUD_ID, value)
            }.apply()
        }

    private companion object {
        const val KEY_CLOUD_ID = "CloudID"
    }
}

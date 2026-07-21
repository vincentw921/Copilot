package com.vincentwang.copilot

import android.app.Application
import com.google.firebase.FirebaseApp
import com.vincentwang.copilot.data.AppPrefs

// Android counterpart of CopilotApp.swift. Firebase plays the role CloudKit
// has on iOS; initialization is a no-op when google-services.json is absent.
class CopilotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPrefs.init(this)
        runCatching { FirebaseApp.initializeApp(this) }
    }

    companion object {
        fun isCloudConfigured(app: Application): Boolean =
            FirebaseApp.getApps(app).isNotEmpty()
    }
}

# Copilot for Android

Android port of the Copilot iOS app (Kotlin + Jetpack Compose). Feature-for-feature
mirror of the iPhone app, with Google's stack standing in for Apple's:

| iOS | Android |
| --- | --- |
| SwiftUI | Jetpack Compose (Material 3) |
| iCloud account (CloudKit auth) | Google account (Firebase Auth + Google Sign-In) |
| Core Data (`NSPersistentCloudKitContainer`) | Room |
| CloudKit private database sync | Cloud Firestore (`users/{uid}/items`) |
| `NSPersistentStoreRemoteChange` observer | Firestore snapshot listener |
| Keychain (cached cloud ID) | `EncryptedSharedPreferences` (Android Keystore) |
| `TabView` (Home/Logbook/Calendar/Report/Profile) | `NavigationBar` tabs |

## Project layout

Mirrors the iOS `Features/` structure:

```
app/src/main/java/com/vincentwang/copilot/
├── CopilotApp.kt            # CopilotApp.swift
├── MainActivity.kt
├── data/                    # Persistence.swift + Copilot.xcdatamodeld
│   ├── Item.kt              # Core Data "Item" entity (all 24 logbook fields)
│   ├── ItemDao.kt
│   ├── CopilotDatabase.kt
│   └── ItemRepository.kt    # Room + Firestore sync (CloudKit equivalent)
├── features/
│   ├── auth/                # AuthModel/AuthView/Keychain
│   ├── home/                # HomeModel/HomeView
│   ├── logbook/  calendar/  aircraft/  profile/  report/
└── ui/
    ├── RootScreen.kt        # RootView
    └── MainTabs.kt          # ContentView
```

## Building

Requires JDK 17+ and the Android SDK (platform 36).

```
cd android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Cloud sync setup (Firebase)

The app builds and runs without any Firebase config — it behaves like the iOS
app when you're signed out of iCloud (local-only, auth screen shows guidance).
To enable sign-in and sync:

1. Create a Firebase project at https://console.firebase.google.com and add an
   Android app with package name `com.vincentwang.copilot`.
2. Enable **Authentication → Google** and **Cloud Firestore**.
3. Download `google-services.json` into `android/app/` (gitignored). The
   Google Services plugin is applied automatically when the file is present.
4. Add your debug keystore's SHA-1 to the Firebase project settings so Google
   Sign-In works: `./gradlew signingReport`.

Data syncs to the signed-in user's private collection `users/{uid}/items`,
one document per logbook entry with the same field names as the Core Data
model, so suggested Firestore security rules are:

```
match /users/{uid}/{document=**} {
  allow read, write: if request.auth != null && request.auth.uid == uid;
}
```

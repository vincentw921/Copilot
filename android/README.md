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
│   ├── Item.kt              # Core Data "Item" entity (all logbook fields)
│   ├── AircraftProfile.kt   # Core Data "Aircraft" entity
│   ├── CopilotDatabase.kt   # Room schema + migrations
│   ├── ItemRepository.kt    # Room + Firestore sync (CloudKit equivalent)
│   └── AppPrefs.kt          # @AppStorage equivalent
├── models/                  # Pure logic, ported from Copilot/Models/
│   ├── FlightEntry.kt  LogbookStats.kt  AircraftCategory.kt
│   ├── AirportDatabase.kt   # assets/airports.csv lookup + haversine
│   ├── CsvExporter.kt  MonthGrid.kt
├── features/
│   ├── auth/                # AuthModel/AuthView/Keychain
│   ├── home/                # dashboard (HomeView.swift)
│   ├── logbook/             # list, entry form, detail sheet, view model
│   ├── calendar/  aircraft/  report/
│   └── profile/             # profile, saved aircraft, settings
└── ui/
    ├── RootScreen.kt        # RootView
    ├── MainTabs.kt          # ContentView
    └── components/          # form sections, calendar, pickers, rows
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

Data syncs to the signed-in user's private collections `users/{uid}/items`
(logbook entries) and `users/{uid}/aircraft` (saved aircraft profiles), one
document per record with the same field names as the Core Data model, so
suggested Firestore security rules are:

```
match /users/{uid}/{document=**} {
  allow read, write: if request.auth != null && request.auth.uid == uid;
}
```

## Releasing to Google Play

Play publishes from a signed **Android App Bundle** (`.aab`). Release builds
are minified with R8 (~3.8 MB vs ~24 MB debug) and stay **unsigned** until
you create a local upload key — signing secrets never live in the repo.

### 1. Create the upload key (once)

```
cd android
keytool -genkeypair -v -keystore upload-keystore.jks -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Then create `android/keystore.properties` (both files are gitignored):

```
storeFile=upload-keystore.jks
storePassword=<your store password>
keyAlias=upload
keyPassword=<your key password>
```

Back the keystore up somewhere safe. With **Play App Signing** (the default
for new apps) Google holds the real app-signing key, so a lost upload key
can be reset — but it's still a day of support-ticket pain.

### 2. Build the release bundle

```
./gradlew :app:bundleRelease        # → app/build/outputs/bundle/release/app-release.aab
```

### 3. Play Console checklist

1. Create the app at https://play.google.com/console ("Copilot Digital
   Logbook", App, Free).
2. Upload `app-release.aab` to an internal-testing track first. Keep
   **Play App Signing** enabled.
3. **After the first upload**: copy the *app signing key* SHA-1 from
   Play Console → Setup → App signing into Firebase → Project settings →
   your Android app → Add fingerprint, then re-download
   `google-services.json` and rebuild. Without this, Google Sign-In fails
   for Play-distributed builds (Play re-signs the app with its own key).
4. Store listing: the 512×512 icon and 1024×500 feature graphic are
   pre-generated in `android/store/`; add at least two phone screenshots
   (grab them with `adb exec-out screencap -p > shot.png`).
5. App content forms:
   - **Privacy policy**: the repo's policy page in `docs/privacy.html`
     (serve it via GitHub Pages and use that URL).
   - **Data safety**: the app collects *User IDs* (Firebase UID) and
     *email/name* (Google Sign-In) for account management, plus app data
     (flight log entries) synced to Firestore; data is encrypted in
     transit; users can delete data in-app (Delete All Flights) and
     request account deletion. No ads, no data sold or shared.
   - **Ads**: none. **Target audience**: 18+ (or 13+; not child-directed).
6. Roll out internal testing → closed/open testing → production.

### Version bumps

`versionCode` must increase on every Play upload; `versionName` is what
users see. Both live in `app/build.gradle.kts` under `defaultConfig`.

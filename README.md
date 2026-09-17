# LifeOS Android

LifeOS is a personal operating system app built with Kotlin and Jetpack Compose.
The first app shell includes a dashboard, task management, calendar events, notes,
expense tracking, reminders, and profile settings.

## Prerequisites

- Android Studio installed with the Android SDK.
- Android SDK Platform 35 and build-tools 35.0.0.
- Java 21. The validated runtime is:
  `C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot`
- An emulator or USB-connected Android device for `installDebug`.

## Build

In PowerShell from this folder:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
.\gradlew.bat assembleDebug
```

The APK is created at `app\build\outputs\apk\debug\app-debug.apk`.

To install on a running emulator or connected device:

```powershell
.\gradlew.bat installDebug
```

VS Code tasks named **Android: assemble debug** and **Android: install debug** are available from the Tasks menu.

## Firebase

The app uses `FirebaseLifeOsRepository`. It starts with local starter data and then
syncs tasks from Firestore after anonymous authentication completes.

## Offline-first storage

LifeOS now saves its current snapshot locally on the device and reads that local
snapshot first, so the app can be used without a network connection. Firebase is
not written automatically. Open **Profile → Connected services → Firebase sync**
and press **Sync now** when you want to back up the device data to Firestore.

Firebase Authentication keeps the active session on the device by default. After a
successful sign-in, the app can reopen and work offline until the user signs out
or Android app data is manually cleared. Signing out clears the active session;
clearing app data removes both the session and local LifeOS data.

Manual sync uploads tasks, events, notes, expenses, and reminders under the signed-in
user's UID. A signed-in Firebase account is required for the sync action.

Use **Profile → Connected services → Restore from Firebase** to replace the local
snapshot with the latest manual backup. The module Add actions and Profile settings
save locally immediately; they are included in the next manual sync.

To connect Firebase:

1. Create an Android Firebase project with application ID `com.lifeos.app`.
2. Download `google-services.json` into `app/`.
3. In Firebase Console, enable **Authentication > Sign-in method > Email/Password**.
4. Create a Cloud Firestore database in test mode while developing.
5. Firebase Storage is not required for the current app and is intentionally unused.

Sign-up creates an email/password Firebase account and stores the profile at
`users/{uid}`. The username lookup is stored at `usernames/{lowercaseUsername}`
so sign-in can accept either email or username. Use Firestore security rules that
require `request.auth.uid == userId` for user-owned documents before production.
The starter rules are in `firestore.rules`.

Tasks are stored at:

```text
users/{anonymousUserId}/tasks/{taskId}
```

Do not commit `google-services.json` if the repository is public.

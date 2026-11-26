# MC Launcher (Android, Kotlin + Jetpack Compose)

Features:
- Offline accounts (UUID derived the same way Minecraft does for offline mode)
- Instances with per-instance `mods/` folders and render settings
- `.jar` mod import and removal via the system file picker
- Version downloads from Mojang's manifest, with SHA-1 verification
- Launch config builder for an on-device Java runtime

## Status

- **Launch needs a runtime.** `MainActivity` passes `UnconfiguredRuntime`, so Launch downloads
  the game and then reports "No Java runtime is connected yet." Implement `RuntimeLauncher`
  with your chosen runtime (for example, a PojavLauncher-based engine) and pass it in.
- Minecraft Java Edition does not run natively on Android. You need your own legitimate copy.
- Natives (LWJGL libraries) are not downloaded yet. Your runtime must supply them.
- Not yet compiled. Expect small fixes on the first build in Android Studio.

## Dependencies (app/build.gradle.kts)

```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3:1.3.0")
}
```

Also add `<uses-permission android:name="android.permission.INTERNET" />` to `AndroidManifest.xml`
so version downloads work.

## Layout

```
app/src/main/java/com/example/mclauncher/
├── MainActivity.kt
├── model/Models.kt
├── account/OfflineAccounts.kt
├── instance/InstanceManager.kt
├── mods/ModManager.kt
├── version/VersionManager.kt
├── launch/LaunchConfig.kt
├── launch/RuntimeLauncher.kt
└── ui/MainScreen.kt
```

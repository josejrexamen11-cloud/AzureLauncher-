# MC Launcher (Android, Kotlin + Jetpack Compose)

A starter launcher with:
- Offline accounts (UUID derived the same way Minecraft does for offline mode)
- Instances with per-instance mods folders and render settings
- Mod (.jar) import and removal
- A launch configuration builder for an on-device Java runtime

## Requirements

- Android Studio (Hedgehog or newer), JDK 17
- Minecraft Java Edition does not run natively on Android. `LaunchBuilder` produces the
  arguments, environment, and game directory for an on-device Java runtime (for example, one
  based on PojavLauncher's engine). You need your own legitimate copy of Minecraft.

## Dependencies (app/build.gradle.kts)

```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.compose.material3:material3:1.3.0")
}
```

## Wiring

In `MainActivity`:

```kotlin
val manager = InstanceManager(this)
setContent { MainScreen(manager) { inst -> /* build LaunchConfig and start runtime */ } }
```

## Next steps
- Wire `ModManager.importMod` to `ActivityResultContracts.OpenDocument()` with `application/java-archive`.
- Install Fabric/Quilt via their profile JSON; Forge needs its installer run in the runtime.
- Fetch the version manifest from Mojang's launcher-meta endpoint and verify SHA-1 hashes.

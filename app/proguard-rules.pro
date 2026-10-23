# RoadForge Launcher — ProGuard / R8 rules

# ─── Keep Room entities + DAOs ───────────────────────────────────
-keep class com.roadforge.launcher.data.** { *; }

# ─── Keep Kotlin data classes used with Gson ─────────────────────
-keep class com.roadforge.launcher.data.RuntimeConfig { *; }
-keep class com.roadforge.launcher.data.TouchLayout   { *; }
-keep class com.roadforge.launcher.data.TouchButton   { *; }
-keep class com.roadforge.launcher.data.InputMapping  { *; }

# ─── Gson ────────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# ─── Kotlin coroutines ───────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ─── Jetpack Compose ─────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ─── Navigation ──────────────────────────────────────────────────
-keep class androidx.navigation.** { *; }

# ─── DataStore ───────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }

# ─── General Android ─────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

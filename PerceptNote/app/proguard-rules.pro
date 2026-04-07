# 📄 app/proguard-rules.pro

# === RETROFIT + GSON ===
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.perceptnote.data.remote.dto.** { *; }
-keep class retrofit2.** { *; }
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# === ROOM ===
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# === HILT ===
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# === ML KIT ===
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# === CameraX ===
-keep class androidx.camera.** { *; }

# === Kotlin Coroutines ===
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

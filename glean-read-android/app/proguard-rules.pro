# ============================================================
# GleanRead Release ProGuard Rules
# ============================================================

# --- Kotlin Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.gleanread.android.**$$serializer { *; }
-keepclassmembers class com.gleanread.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.gleanread.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Ktor ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# --- Supabase ---
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# --- Coil ---
-keep class coil3.** { *; }
-dontwarn coil3.**

# --- General Android ---
-keep class * extends android.app.Activity
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider

# --- Compose ---
-dontwarn androidx.compose.**

# --- Keep data classes used for serialization ---
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

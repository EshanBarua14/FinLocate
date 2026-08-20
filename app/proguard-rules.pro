# ====================================================================
# ENTERPRISE PROGUARD / R8 RULES FOR PRODUCTION DISTRIBUTION
# ====================================================================

# --------------------------------------------------------------------
# 1. Reflection, Attributes & Line Number Preservation
# --------------------------------------------------------------------
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# --------------------------------------------------------------------
# 2. Room Database Entities, DAOs, Database & Migrations
# --------------------------------------------------------------------
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

-keep class com.example.data.database.** { *; }
-keepclassmembers class com.example.data.database.** { *; }

-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>();
}
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.Migration { *; }
-keep interface androidx.room.RoomMasterTable { *; }

# --------------------------------------------------------------------
# 3. Moshi Adapters, JSON CodeGen & Data Models
# --------------------------------------------------------------------
-keep class com.example.data.api.** { *; }
-keepclassmembers class com.example.data.api.** { *; }

-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}
-keep class * extends com.squareup.moshi.JsonAdapter
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    <init>(...);
}
-dontwarn com.squareup.moshi.**

# --------------------------------------------------------------------
# 4. Firebase Dependencies (Crashlytics, Analytics, AI)
# --------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firebase Crashlytics specific
-keepclassmembers class com.google.firebase.crashlytics.** { *; }

# --------------------------------------------------------------------
# 5. Network Stack (Retrofit & OkHttp)
# --------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# --------------------------------------------------------------------
# 6. Kotlin Coroutines & WorkManager
# --------------------------------------------------------------------
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

-keep class androidx.work.** { *; }
-keepclassmembers class androidx.work.** { *; }

# --------------------------------------------------------------------
# 7. Jetpack Compose & UI Preservation
# --------------------------------------------------------------------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

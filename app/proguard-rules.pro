# Add project specific ProGuard rules here.

# Keep Room entities and domain models
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

# Keep Room DAOs and Database instance
-keep class com.example.data.database.** { *; }
-keepclassmembers class com.example.data.database.** { *; }

# Keep API Request and Response Data Models
-keep class com.example.data.api.** { *; }
-keepclassmembers class com.example.data.api.** { *; }

# Room framework preservation
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>();
}
-dontwarn androidx.room.paging.**

# Preserve annotations and signatures for Reflection & Serialization
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-dontwarn okhttp3.**
-dontwarn retrofit2.**


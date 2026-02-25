# Sukoon Music Player ProGuard Configuration
# Preserves classes and members used via reflection by R8

# Room database entities — prevent R8 from renaming fields
-keep class com.sukoon.music.data.local.entity.** { *; }
-keep class com.sukoon.music.data.local.dao.** { *; }

# Retrofit / Gson model classes
-keep class com.sukoon.music.data.remote.model.** { *; }
-keep class com.sukoon.music.data.remote.dto.** { *; }
-keepclassmembers class com.sukoon.music.data.remote.** { *; }

# Hilt — keep generated components and entry points
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }
-keepclasseswithmembernames class * { @dagger.hilt.* <methods>; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }

# Firebase Firestore model classes (used for feedback)
-keep class com.sukoon.music.data.feedback.** { *; }
-keepclassmembers class com.sukoon.music.data.feedback.** { *; }

# Firebase Analytics and Config (reflection-heavy)
-keep class com.google.firebase.analytics.** { *; }

# Preserve source file names and line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep enum values() and valueOf() methods (used by reflection)
-keepclassmembers enum * { *; }
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===== Kotlin Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ===== Kotlin Reflection =====
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# ===== Firebase Realtime Database =====
# Keep data models for Firebase
-keep @androidx.annotation.Keep class * { *; }
-keep class com.reflection.thecampus.Course { *; }
-keep class com.reflection.thecampus.BasicInfo { *; }
-keep class com.reflection.thecampus.Pricing { *; }
-keep class com.reflection.thecampus.Schedule { *; }
-keep class com.reflection.thecampus.Announcement { *; }
-keep class com.reflection.thecampus.Faculty { *; }
-keep class com.reflection.thecampus.Test { *; }
-keep class com.reflection.thecampus.UserProfile { *; }
-keep class com.reflection.thecampus.AnnouncementItem { *; }
-keep class com.reflection.thecampus.data.model.** { *; }

# Keep all data classes in the main package
-keep class com.reflection.thecampus.** extends java.io.Serializable { *; }
-keep class com.reflection.thecampus.** implements android.os.Parcelable { *; }

# Keep Firebase-related fields and methods
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
}

# Keep classes with @IgnoreExtraProperties
-keep @com.google.firebase.database.IgnoreExtraProperties class * { *; }

# Keep all constructors and default values for Firebase deserialization
-keepclassmembers class com.reflection.thecampus.** {
    public <init>(...);
    *** Companion;
}

# Keep companion objects
-keep class com.reflection.thecampus.**$Companion { *; }

# ===== Parcelable implementation =====
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep kotlinx.parcelize
-keep class kotlinx.parcelize.Parcelize
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ===== Room Database =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Room DAOs
-keep interface com.reflection.thecampus.data.local.** { *; }
-keep class com.reflection.thecampus.data.local.** { *; }

# ===== ViewModel & LiveData =====
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# ===== Cashfree Payment SDK =====
-keep class com.cashfree.pg.** { *; }
-dontwarn com.cashfree.pg.**

# ===== OkHttp & Retrofit =====
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ===== Timber =====
-dontwarn org.jetbrains.annotations.**
-keep class timber.log.** { *; }

# ===== Glide =====
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# ===== Gson =====
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ===== General Android =====
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# Keep native methods
-keepclassmembers class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# ===== Enum Classes =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== WorkManager =====
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep class androidx.work.impl.** { *; }

# ===== Firebase Crashlytics =====
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Keep ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep OkHttp
-keep class okhttp3.** { *; }
-keep class okhttp3.internal.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Gson
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-dontwarn com.google.gson.**

# Keep BouncyCastle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep data classes
-keepclassmembers class com.katafract.safeopen.models.** {
    <init>(...);
    *** get*();
    void set*(...);
}

# Keep Activity and AppCompat
-keep class androidx.appcompat.app.** { *; }
-keep class androidx.activity.** { *; }

# Errorprone
-dontwarn com.google.errorprone.annotations.**

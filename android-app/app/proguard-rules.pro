# Proguard rules for SwasthAI Report Generator

# Keep all data model classes and their fields for Gson serialization/deserialization
-keep class com.piyush.swasthaireportgenerator.data.model.** { *; }
-keepclassmembers class com.piyush.swasthaireportgenerator.data.model.** { *; }

# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Retrofit rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Coroutines rules
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }


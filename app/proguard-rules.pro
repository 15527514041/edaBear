# 易达熊 ProGuard 混淆规则

# ── MediaPipe 模型文件 ──
-keep class com.google.mediapipe.** { *; }
-keep class org.tensorflow.lite.** { *; }

# ── Kotlin Serialization ──
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.yidaxiong.app.**$$serializer { *; }
-keepclassmembers class com.yidaxiong.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.yidaxiong.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Hilt / Dagger ──
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# ── Retrofit ──
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ── OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**

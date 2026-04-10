# LiveConnect Chat SDK - Consumer ProGuard Rules

# Keep public API
-keep class com.techindika.liveconnect.LiveConnectChat { *; }
-keep class com.techindika.liveconnect.LiveConnectTheme { *; }
-keep class com.techindika.liveconnect.LiveConnectTheme$Builder { *; }
-keep class com.techindika.liveconnect.model.** { *; }
-keep class com.techindika.liveconnect.callback.** { *; }
-keep class com.techindika.liveconnect.ui.view.FloatingChatButton { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Socket.IO
-keep class io.socket.** { *; }
-dontwarn io.socket.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }

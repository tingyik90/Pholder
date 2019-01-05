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
# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
# For Crashlytics, see https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

# Glide requirement
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder
# See https://github.com/smooch/smooch-android/issues/93
-keepclassmembers enum * {
public static **[] values();
public static ** valueOf(java.lang.String);
}

# Prevent SearchView NPE, see https://stackoverflow.com/a/23934038/3584439
#-keep class android.support.v7.widget.SearchView { *; }

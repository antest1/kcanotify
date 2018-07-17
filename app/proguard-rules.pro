# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\alias\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontobfuscate
-dontskipnonpubliclibraryclasses
-keepattributes SoureFile,LineNumberTable
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

-keepattributes Signature
-keepattributes *Annotation*

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**
-dontwarn com.google.**

-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keepclassmembernames,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn org.conscrypt.**

-dontwarn org.apache.commons.**
-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**
-dontwarn org.apache.log4j.**

# Gson
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

-dontwarn oauth.**
-keep class com.woxthebox.draglistview.** { *; }
-keep class com.github.mikephil.charting.** { *; }

-keepattributes SourceFile,LineNumberTable
-keep class org.acra.** { *; }

-dontwarn android.test.**
-dontwarn java.lang.invoke.**
# -keep public class android.widget.** { *; }

#Support library
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }

-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}

# Design
-dontwarn android.support.**
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }

# Guava
-keep class com.google.common.base.** {*;}

-keepclassmembers class **.R$* {
    public static <fields>;
}

-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.**
-keep class com.google.zxing.** { *; }
-keep class com.google.journeyapps.** { *; }

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** e(...);
    public static *** w(...);
    public static *** wtf(...);
}

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-keep class com.antest1.kcanotify.** { *; }
-keepnames class com.antest1.kcanotify.** { *; }

#NetGuard
-keepnames class eu.faircode.netguard.** { *; }

#JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

#JNI callbacks
-keep class eu.faircode.netguard.** { *; }
-keep class eu.faircode.netguard.IPUtil { *; }
-keep class eu.faircode.netguard.Packet { *; }
-keep class eu.faircode.netguard.ResourceRecord { *; }
-keep class eu.faircode.netguard.Rule { *; }
-keep class eu.faircode.netguard.Util { *; }

-keep class com.antest1.kcanotify.KcaVpnService {
    void nativeExit(java.lang.String);
    void nativeError(int, java.lang.String);
    void dnsResolved(eu.faircode.netguard.ResourceRecord);
}

-keep class com.antest1.kcanotify.KcaVpnData {
    int containsKcaServer(int, byte[], byte[]);
    void getDataFromNative(byte[], int, int, byte[], byte[], int, int);
 }


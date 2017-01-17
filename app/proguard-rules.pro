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

-dontwarn io.netty.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

-dontwarn org.apache.commons.**
-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**
-dontwarn org.apache.log4j.**
-dontwarn org.littleshoot.**

# Jzlib
-keep class com.jcraft.jzlib.** { *; }
-keep interface com.jcraft.jzlib.** { *; }

-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-dontwarn oauth.**
-dontwarn com.androidquery.auth.**

-keepattributes SourceFile,LineNumberTable
-keep class org.acra.** { *; }

-dontwarn android.test.**
-keep public class android.widget.** { *; }

#Support library
-keep class android.support.v7.widget.** { *; }
-dontwarn android.support.v4.**


-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** e(...);
    public static *** w(...);
    public static *** wtf(...);
}

-keepnames class com.antest1.kcanotify.** { *; }


#NetGuard
-keepnames class eu.faircode.netguard.** { *; }

#JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

#JNI callbacks
-keep class eu.faircode.netguard.Allowed { *; }
-keep class eu.faircode.netguard.Packet { *; }
-keep class eu.faircode.netguard.ResourceRecord { *; }
-keep class eu.faircode.netguard.Usage { *; }
-keep class com.antest1.kcanotify.KcaVpnService {
    void nativeExit(java.lang.String);
    void nativeError(int, java.lang.String);
    void logPacket(eu.faircode.netguard.Packet);
    void dnsResolved(eu.faircode.netguard.ResourceRecord);
    boolean isDomainBlocked(java.lang.String);
    eu.faircode.netguard.Allowed isAddressAllowed(eu.faircode.netguard.Packet);
    void accountUsage(eu.faircode.netguard.Usage);
}

-keep class com.antest1.kcanotify.KcaVpnData {
    int containsKcaServer(byte[], byte[]);
    void getDataFromNative(byte[], int, int, byte[], byte[]);
 }


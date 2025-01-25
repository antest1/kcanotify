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
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keep public class * extends java.lang.Exception

-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**
-dontwarn com.google.**

-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn org.conscrypt.**

-dontwarn org.apache.commons.**
-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**
-dontwarn org.apache.log4j.**

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken


# Retrofit
# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

-dontwarn oauth.**
-keep class com.woxthebox.draglistview.** { *; }
-keep class com.github.mikephil.charting.** { *; }

-keepattributes SourceFile,LineNumberTable

-dontwarn android.test.**
-dontwarn java.lang.invoke.**
# -keep public class android.widget.** { *; }

#Support library
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }

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

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

-keep class com.google.firebase.installations.** {
  *;
}

-keep interface com.google.firebase.installations.** {
  *;
}

-keep class com.antest1.kcanotify.** { *; }
-keepnames class com.antest1.kcanotify.** { *; }

#JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.antest1.kcanotify.KcaVpnData {
    int containsKcaServer(int, byte[], byte[], byte[]);
    void getDataFromNative(byte[], int, int, byte[], byte[], int, int);
}

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** e(...);
    public static *** w(...);
    public static *** wtf(...);
}

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontwarn android.support.**
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!class/unboxing/enum

-keepattributes Signature,InnerClasses
-keepattributes SourceFile,LineNumberTable

-dontobfuscate

-keep class com.toolsboox.** { *; }

-keep class com.google.android.gms.** { *; }
-dontnote com.google.android.gms.**
-dontwarn com.google.android.gms.**

-keep class com.google.firebase.** { *; }
-dontnote com.google.firebase.**
-dontwarn com.google.firebase.**

-keep class kotlin.** { *; }
-dontnote kotlin.**
-dontwarn kotlin.**

-keep class kotlinx.** { *; }
-dontnote kotlinx.**
-dontwarn kotlinx.**

-dontwarn javax.annotation.**

-keep class okhttp3.** { *; }
-dontnote okhttp3.**
-dontwarn okhttp3.**

-keep class okio.** { *; }
-dontnote okio.**
-dontwarn okio.**

-keep class retrofit2.** { *; }
-dontnote retrofit2.**
-dontwarn retrofit2.**

-keep class com.google.gson.** { *; }
-dontnote com.google.gson.**
-dontwarn com.google.gson.**

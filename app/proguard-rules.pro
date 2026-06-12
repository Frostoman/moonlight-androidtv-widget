# Minification is disabled by default, but keep the crypto + HTTP libs safe if enabled.
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class org.xmlpull.** { *; }

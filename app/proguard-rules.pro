# Keep Room entities, DAOs and generated impls intact under R8.
-keep class com.erfanbagheri.tahdig.data.local.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { <init>(); }

# kotlinx.serialization models used by the seed loader.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.erfanbagheri.tahdig.data.local.seed.** {
    *** Companion;
}
-keepclasseswithmembers class com.erfanbagheri.tahdig.data.local.seed.** {
    kotlinx.serialization.KSerializer serializer(...);
}

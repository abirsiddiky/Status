# ProGuard / R8 rules for Status app

# kotlinx.serialization
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-keepclassmembers class * {
    companion ref;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembernames class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    @kotlinx.serialization.Serializable class *;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

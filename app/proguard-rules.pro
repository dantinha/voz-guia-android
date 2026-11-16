# Proguard / R8 rules for Voz Guia
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn java.lang.invoke.**

# Android Entry Points (Manifest components)
-keep public class br.com.vozguia.VozGuiaApplication
-keep public class br.com.vozguia.presentation.MainActivity
-keep public class br.com.vozguia.service.accessibility.VozGuiaAccessibilityService
-keep public class br.com.vozguia.service.notification.VozGuiaNotificationListenerService

# Room Persistence (Entities, DAOs, TypeConverters, Database)
-keep class * extends androidx.room.RoomDatabase
-keep class br.com.vozguia.core.routine.data.room.** { *; }
-keepclasseswithmembers class * {
    @androidx.room.* <methods>;
}

# AndroidX Security Crypto / Tink
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy

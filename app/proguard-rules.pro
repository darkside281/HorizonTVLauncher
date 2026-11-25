# Keep Horizon TV Launcher entry points & services
-keep class com.horizontv.launcher.MainActivity { *; }
-keep class com.horizontv.launcher.HorizonDreamService { *; }

# Keep SMBJ & BouncyCastle classes loaded via reflection for SMB2/SMB3/NTLM auth
-keep class com.hierynomus.smbj.** { *; }
-keep class com.hierynomus.security.bc.** { *; }
-keep class com.hierynomus.security.jce.** { *; }
-keep class com.hierynomus.ntlm.** { *; }
-keep class com.hierynomus.spnego.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }
-keep class org.bouncycastle.jcajce.provider.symmetric.AES** { *; }
-keep class org.bouncycastle.jcajce.provider.digest.MD4** { *; }
-keep class org.bouncycastle.jcajce.provider.digest.MD5** { *; }
-keep class org.bouncycastle.jcajce.provider.digest.SHA256** { *; }
-keep class org.bouncycastle.jcajce.provider.digest.SHA512** { *; }
-keep class org.bouncycastle.crypto.digests.** { *; }
-keep class org.bouncycastle.crypto.engines.AES** { *; }
-keep class org.bouncycastle.crypto.macs.** { *; }

# Silence harmless optional SLF4J / BouncyCastle desktop warnings in R8
-dontwarn org.slf4j.**
-dontwarn org.bouncycastle.**
-dontwarn com.hierynomus.**
-dontwarn javax.naming.**
-dontwarn javax.el.**
-dontwarn net.engio.mbassy.**
-dontwarn java.lang.management.**

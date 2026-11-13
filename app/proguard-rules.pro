# Keep accessibility service, tile, receivers (referenced from manifest, AGP keeps them,
# but be explicit for safety with aggressive optimization).
-keep class com.shishir.appdns.core.** { *; }
-keep class com.shishir.appdns.App { *; }

# Compose
-dontwarn androidx.compose.**

# Kotlin metadata
-keepattributes *Annotation*, InnerClasses, Signature
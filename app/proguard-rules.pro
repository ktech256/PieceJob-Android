# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep PieceJob Core communication and socket classes
-keep class com.piecejob.core.socket.** { *; }
-keep class com.piecejob.core.communication.** { *; }
-keep class com.piecejob.core.data.remote.dto.** { *; }

# Keep Dagger Hilt
-keep class * extends androidx.lifecycle.ViewModel
-keep class * { @dagger.hilt.android.lifecycle.HiltViewModel <methods>; }
-keep class * { @javax.inject.Inject <methods>; }

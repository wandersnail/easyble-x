#包明不混合大小写
-dontusemixedcaseclassnames  
# 混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
#保护注解
-keepattributes Signature,Exceptions,InnerClasses,*Annotation*
-dontshrink
# 保持哪些类不被混淆
-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep class android.** {*;}
-keep class kotlin.** {*;}
-keep class kotlinx.** {*;}
-keep class javax.** {*;}
-keep class java.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**
-dontwarn android.support.**
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keepclasseswithmembers @kotlin.Metadata class * { *; }
-keepclassmembers class **.WhenMappings {
    <fields>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}
-dontwarn kotlin.**
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.google.vending.licensing.ILicensingService
-keep public class com.android.vending.licensing.ILicensingService
#如果有引用v4包可以添加下面这行
-keep public class * extends android.support.v4.app.Fragment
-keepclassmembers class * {
   public <init>(org.json.JSONObject);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep public class * extends android.view.View {
	public <init>(android.content.Context);
	public <init>(android.content.Context, android.util.AttributeSet);
	public <init>(android.content.Context, android.util.AttributeSet, int);
	void set*(***);
    *** get*();
}

-keep class android.support.annotation.Keep
-keep class androidx.annotation.Keep
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}

-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}

-keepclasseswithmembers class * {
    @androidx.annotation.Keep <init>(...);
}
-keepclasseswithmembers class * {
    @android.support.annotation.Keep <methods>;
}

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <fields>;
}

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <init>(...);
}

#保持 native 方法不被混淆
-keepclasseswithmembernames class * {
	native <methods>;
}

# Keep native methods
-keepclassmembers class * {
    native <methods>;
}

#保持自定义控件类不被混淆
-keepclasseswithmembers class * {
	public <init>(android.content.Context, android.util.AttributeSet);
}

#保持自定义控件类不被混淆
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

#保持 Parcelable 不被混淆
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

#保持 Serializable 不被混淆
-keepnames class * implements java.io.Serializable

#保持 Serializable 不被混淆并且enum 类也不被混淆
-keepclassmembers class * implements java.io.Serializable {
	static final long serialVersionUID;
	private static final java.io.ObjectStreamField[] serialPersistentFields;
	!static !transient <fields>;
	!private <fields>;
	!private <methods>;
	private void writeObject(java.io.ObjectOutputStream);
	private void readObject(java.io.ObjectInputStream);
	java.lang.Object writeReplace();
	java.lang.Object readResolve();
}
#不混淆资源类
-keepclassmembers class **.R$* {
	public static <fields>;
}

-keep class * implements cn.wandersnail.commons.observer.Observe {
	public <methods>;
}
-keep class * implements cn.wandersnail.ble.Request {
    !private *;
}
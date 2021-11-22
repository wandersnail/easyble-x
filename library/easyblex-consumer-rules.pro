# 混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
#保护注解
-keepattributes Signature,Exceptions,InnerClasses,*Annotation*

-keep class * implements cn.wandersnail.commons.observer.Observe {
	public <methods>;
}
-keep class * implements cn.wandersnail.ble.Request {
    !private *;
}
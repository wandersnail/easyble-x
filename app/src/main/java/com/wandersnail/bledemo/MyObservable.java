package com.wandersnail.bledemo;

import com.snail.commons.methodpost.MethodInfo;
import easyble2.EventObservable;

/**
 * date: 2019/8/11 00:34
 * author: zengfansheng
 */
public class MyObservable extends EventObservable {
    public void nofityTestObserver() {
        notifyObservers(new MethodInfo("testObserver", new MethodInfo.Parameter(MyEvent.class, new MyEvent("测试观察者"))));
    }
}

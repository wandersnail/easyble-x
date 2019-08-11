package easyble2;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * date: 2019/8/9 16:19
 * author: zengfansheng
 */
class ObserverInfo {
    final WeakReference<EventObserver> weakObserver;
    final Map<String, ObserverMethod> methodMap;

    ObserverInfo(EventObserver observer, Map<String, ObserverMethod> methodMap) {
        weakObserver = new WeakReference<>(observer);
        this.methodMap = methodMap;
    }
}

package easyble2;

import com.snail.commons.methodpost.MethodInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 消息发布者、被观察者
 * <p>
 * date: 2019/8/3 13:14
 * author: zengfansheng
 */
public class EventObservable {
    private final List<ObserverInfo> observerInfos = new ArrayList<>();
    private EasyBLE easyBle;

    void setEasyBLE(EasyBLE easyBle) {
        this.easyBle = easyBle;
    }

    /**
     * 将观察者添加到注册集合里
     *
     * @param observer 需要注册的观察者
     */
    void registerObserver(@NonNull EventObserver observer) {
        Inspector.requireNonNull(observer, "observer is null");
        synchronized (observerInfos) {
            boolean registered = false;
            for (Iterator<ObserverInfo> it = observerInfos.iterator(); it.hasNext(); ) {
                ObserverInfo info = it.next();
                EventObserver o = info.weakObserver.get();
                if (o == null) {
                    it.remove();
                } else if (o == observer) {
                    registered = true;
                }
            }
            if (registered) {
                throw new EasyBLEException("Observer " + observer + " is already registered.");
            }
            Map<String, ObserverMethod> methodMap = ObserverMethodUtils.findObserverMethod(observer, easyBle);
            observerInfos.add(new ObserverInfo(observer, methodMap));
        }
    }

    /**
     * 查询观察者是否注册
     *
     * @param observer 要查询的观察者
     */
    boolean isRegistered(@NonNull EventObserver observer) {
        synchronized (observerInfos) {
            for (ObserverInfo info : observerInfos) {
                if (info.weakObserver.get() == observer) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 将观察者从注册集合里移除
     *
     * @param observer 需要取消注册的观察者
     */
    void unregisterObserver(@NonNull EventObserver observer) {
        synchronized (observerInfos) {
            for (Iterator<ObserverInfo> it = observerInfos.iterator(); it.hasNext(); ) {
                ObserverInfo info = it.next();
                EventObserver o = info.weakObserver.get();
                if (o == null || observer == o) {
                    it.remove();
                }
            }
        }
    }

    /**
     * 将所有观察者从注册集合中移除
     */
    void unregisterAll() {
        synchronized (observerInfos) {
            observerInfos.clear();
        }
        ObserverMethodUtils.clearCache();
    }

    private List<ObserverInfo> getObserverInfos() {
        synchronized (observerInfos) {
            ArrayList<ObserverInfo> infos = new ArrayList<>();
            for (ObserverInfo info : observerInfos) {
                EventObserver observer = info.weakObserver.get();
                if (observer != null) {
                    infos.add(info);
                }
            }
            return infos;
        }
    }

    /**
     * 通知所有观察者事件变化
     *
     * @param methodName 要调用观察者的方法名
     * @param parameters 方法参数信息对
     */
    public void notifyObservers(@NonNull String methodName, @Nullable MethodInfo.Parameter... parameters) {
        notifyObservers(new MethodInfo(methodName, parameters));
    }

    /**
     * 通知所有观察者事件变化
     *
     * @param info 方法信息实例
     */
    public void notifyObservers(@NonNull MethodInfo info) {
        List<ObserverInfo> infos = getObserverInfos();
        for (ObserverInfo oi : infos) {
            EventObserver observer = oi.weakObserver.get();
            if (observer != null) {
                String key = ObserverMethodUtils.getMethodString(info);
                ObserverMethod observerMethod = oi.methodMap.get(key);
                if (observerMethod != null) {
                    Runnable runnable = ObserverMethodUtils.generateRunnable(observer, observerMethod.getMethod(), info);
                    easyBle.getPosterDispatcher().post(observerMethod.getThreadMode(), runnable);
                }
            }
        }
    }
}

package easyble2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 消息发布者、被观察者
 * <p>
 * date: 2019/8/3 13:14
 * author: zengfansheng
 */
public class EventObservable {
    private final List<WeakReference<EventObserver>> observers = new ArrayList<>();
    Poster poster;
        
    /**
     * 将观察者添加到注册集合里
     *
     * @param observer 需要注册的观察者
     */
    void registerObserver(@NonNull EventObserver observer) {
        Inspector.requireNonNull(observer, "observer is null");
        synchronized (observers) {
            for (WeakReference<EventObserver> it : observers) {
                if (observer == it.get()) {
                    throw new EasyBLEException("Observer " + observer + " is already registered.");
                }
            }
            observers.add(new WeakReference<>(observer));
        }
    }

    /**
     * 查询观察者是否注册
     *
     * @param observer 要查询的观察者
     */
    boolean isRegistered(@NonNull EventObserver observer) {
        synchronized (observers) {
            for (WeakReference<EventObserver> it : observers) {
                if (observer == it.get()) {
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
        synchronized (observers) {
            Iterator<WeakReference<EventObserver>> iterator = observers.iterator();
            while (iterator.hasNext()) {
                WeakReference<EventObserver> it = iterator.next();
                if (it.get() == observer) {
                    iterator.remove();
                    break;
                }
            }
        }
    }

    /**
     * 将所有观察者从注册集合中移除
     */
    void unregisterAll() {
        synchronized (observers) {
            observers.clear();
        }
    }

    public EventObserver[] getObservers() {
        synchronized (observers) {
            ArrayList<EventObserver> obs = new ArrayList<>();
            Iterator<WeakReference<EventObserver>> iterator = observers.iterator();
            while (iterator.hasNext()) {
                WeakReference<EventObserver> it = iterator.next();
                EventObserver observer = it.get();
                if (observer == null) {
                    iterator.remove();
                } else {
                    obs.add(observer);
                }
            }
            return (EventObserver[]) obs.toArray();
        }
    }

    /**
     * 通知所有观察者事件变化
     *
     * @param methodName 要调用观察者的方法名
     * @param pairs      方法参数信息对
     */
    public void notifyObservers(@NonNull String methodName, @Nullable TypeValuePair... pairs) {
        EventObserver[] obs = getObservers();
        for (EventObserver observer : obs) {
            poster.post(observer, methodName, pairs);
        }
    }

    /**
     * 通知所有观察者事件变化
     *
     * @param info 方法信息实例
     */
    public void notifyObservers(@NonNull MethodInfo info) {
        EventObserver[] obs = getObservers();
        for (EventObserver observer : obs) {
            poster.post(observer, info);
        }
    }    
}

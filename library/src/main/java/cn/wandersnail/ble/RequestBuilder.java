package cn.wandersnail.ble;

import java.util.UUID;

import cn.wandersnail.ble.callback.RequestCallback;
import cn.wandersnail.commons.observer.Observe;
import cn.wandersnail.commons.poster.RunOn;

/**
 * date: 2019/9/20 18:00
 * author: zengfansheng
 */
public class RequestBuilder<T extends RequestCallback> {
    String tag;
    RequestType type;
    UUID service;
    UUID characteristic;
    UUID descriptor;
    Object value;
    int priority;
    RequestCallback callback;
    WriteOptions writeOptions;

    RequestBuilder(RequestType type) {
        this.type = type;
    }

    /**
     * @param tag 请求标识，用于标识每次请求，规则自定。可以用来区分相同类型的不同批次请求
     */
    public RequestBuilder<T> setTag(String tag) {
        this.tag = tag;
        return this;
    }

    /**
     * @param priority 请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    public RequestBuilder<T> setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * 如果设置了回调，则观察者不会收到此次请求的消息；不设置则使用观察者接收请求结果。
     * <br>回调方法使用{@link RunOn}注解指定执行线程，观察者方法使用{@link Observe}注解指定执行线程
     */
    public RequestBuilder<T> setCallback(T callback) {
        this.callback = callback;
        return this;
    }

    public Request build() {
        return new GenericRequest(this);
    }
}

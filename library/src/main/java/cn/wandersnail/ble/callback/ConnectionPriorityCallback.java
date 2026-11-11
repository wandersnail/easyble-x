package cn.wandersnail.ble.callback;

import androidx.annotation.NonNull;

import cn.wandersnail.ble.Request;

/**
 * 请求连接优先级回调。
 * <p>
 * 该回调只表示系统是否受理了请求，实际生效的连接参数请通过
 * {@link cn.wandersnail.ble.EventObserver#onConnectionUpdated} 获取。
 * <p>
 * date: 2026/11/11
 * author: zengfansheng
 */
public interface ConnectionPriorityCallback extends RequestFailedCallback {
    /**
     * 连接优先级请求已被系统受理
     *
     * @param request 请求
     */
    void onConnectionPriorityRequested(@NonNull Request request);
}

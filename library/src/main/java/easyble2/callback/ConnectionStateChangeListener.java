package easyble2.callback;

import androidx.annotation.NonNull;
import easyble2.Connection;
import easyble2.Device;

/**
 * date: 2019/8/3 17:41
 * author: zengfansheng
 */
public interface ConnectionStateChangeListener {
    /**
     * 连接状态变化
     *
     * @param device 设备。状态{@link Device#connectionState}，可能的值{@link Connection#STATE_CONNECTED}等
     */
    void onConnectionStateChanged(@NonNull Device device);

    /**
     * 连接失败
     *
     * @param device   设备
     * @param failType 失败类型。{@link Connection#CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION}等
     */
    void onConnectFailed(@NonNull Device device, int failType);

    /**
     * 连接超时
     *
     * @param device 设备
     * @param type   原因。{@link Connection#TIMEOUT_TYPE_CANNOT_CONNECT}
     */
    void onConnectTimeout(@NonNull Device device, int type);
}

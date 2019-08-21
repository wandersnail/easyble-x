package cn.wandersnail.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

import cn.wandersnail.commons.observer.Observer;

/**
 * 各种事件。蓝牙状态，连接状态，读取到特征值，写入结果回调等等
 * <p>
 * date: 2019/8/3 13:15
 * author: zengfansheng
 */
public interface EventObserver extends Observer {
    /**
     * 蓝牙开关状态变化
     *
     * @param state {@link BluetoothAdapter#STATE_OFF}等
     */
    default void onBluetoothAdapterStateChanged(int state) {
    }

    /**
     * 读取到特征值
     *
     * @param request 请求
     * @param value   读取到的数据
     */
    default void onCharacteristicRead(@NonNull Request request, @NonNull byte[] value) {
    }

    /**
     * 特征值变化
     *
     * @param device         设备
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param value          数据
     */
    default void onCharacteristicChanged(@NonNull Device device, @NonNull UUID service, @NonNull UUID characteristic,
                                         @NonNull byte[] value) {
    }

    /**
     * 成功写入特征值
     *
     * @param request 请求
     * @param value   写入的数据
     */
    default void onCharacteristicWrite(@NonNull Request request, @NonNull byte[] value) {
    }

    /**
     * 读取到设备的信号强度
     *
     * @param request 请求
     * @param rssi    信号强度
     */
    default void onRssiRead(@NonNull Request request, int rssi) {
    }

    /**
     * 读取到描述符值
     *
     * @param request 请求
     * @param value   读取到的数据
     */
    default void onDescriptorRead(@NonNull Request request, @NonNull byte[] value) {
    }

    /**
     * 通知开关变化 / Indication开关变化
     *
     * @param request   请求
     * @param isEnabled 开启或关闭
     */
    default void onNotificationChanged(@NonNull Request request, boolean isEnabled) {
    }

    /**
     * 最大传输单元变化
     *
     * @param request 请求
     * @param mtu     最大传输单元新的值
     */
    default void onMtuChanged(@NonNull Request request, int mtu) {
    }

    /**
     * @param request 请求
     * @param txPhy   物理层发送器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param rxPhy   物理层接收器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     */
    default void onPhyChange(@NonNull Request request, int txPhy, int rxPhy) {
    }

    /**
     * 请求失败
     *
     * @param request  请求
     * @param failType 失败类型。{@link Connection#REQUEST_FAIL_TYPE_GATT_IS_NULL}等
     * @param value    请求时带的数据，可能为null
     */
    default void onRequestFailed(@NonNull Request request, int failType, @Nullable Object value) {
    }

    /**
     * 连接状态变化
     *
     * @param device 设备。状态{@link Device#getConnectionState()}，可能的值{@link ConnectionState#CONNECTED}等
     */
    default void onConnectionStateChanged(@NonNull Device device) {
    }

    /**
     * 连接失败
     *
     * @param device   设备
     * @param failType 失败类型。{@link Connection#CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION}等
     */
    default void onConnectFailed(@NonNull Device device, int failType) {
    }

    /**
     * 连接超时
     *
     * @param device 设备
     * @param type   原因。{@link Connection#TIMEOUT_TYPE_CANNOT_CONNECT}
     */
    default void onConnectTimeout(@NonNull Device device, int type) {
    }
}

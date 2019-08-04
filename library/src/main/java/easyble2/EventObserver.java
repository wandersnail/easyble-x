package easyble2;

import android.bluetooth.BluetoothAdapter;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

/**
 * 各种事件。蓝牙状态，连接状态，读取到特征值，写入结果回调等等
 * <p>
 * date: 2019/8/3 13:15
 * author: zengfansheng
 */
public interface EventObserver {
    /**
     * 蓝牙开关状态变化
     *
     * @param state {@link BluetoothAdapter#STATE_OFF}等
     */
    void onBluetoothAdapterStateChanged(int state);

    /**
     * 读取到特征值
     *
     * @param tag         请求标识
     * @param device      设备
     * @param serviceUuid 服务UUID
     * @param characUuid  特征UUID
     * @param value       读取到的数据
     */
    void onCharacteristicRead(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value);

    /**
     * 特征值变化
     *
     * @param device      设备
     * @param serviceUuid 服务UUID
     * @param characUuid  特征UUID
     * @param value       数据
     */
    void onCharacteristicChanged(@NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value);

    /**
     * 读取到特征值
     *
     * @param tag         请求标识
     * @param device      设备
     * @param serviceUuid 服务UUID
     * @param characUuid  特征UUID
     * @param value       读取到的数据
     */
    void onCharacteristicWrite(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value);

    /**
     * 读取到设备的信号强度
     *
     * @param tag    请求标识
     * @param device 设备
     * @param ssid   信号强度
     */
    void onRemoteRssiRead(@Nullable String tag, @NonNull Device device, int ssid);

    /**
     * 读取到描述符值
     *
     * @param tag            请求标识
     * @param device         设备
     * @param serviceUuid    服务UUID
     * @param characUuid     特征UUID
     * @param descriptorUuid 描述符UUID
     * @param value          读取到的数据
     */
    void onDescriptorRead(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid,
                          @NonNull UUID descriptorUuid, @NonNull byte[] value);

    /**
     * 通知开关变化
     *
     * @param tag            请求标识
     * @param device         设备
     * @param serviceUuid    服务UUID
     * @param characUuid     特征UUID
     * @param descriptorUuid 描述符UUID
     * @param isEnabled      开启或关闭
     */
    void onNotificationChanged(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid,
                               @NonNull UUID descriptorUuid, boolean isEnabled);

    /**
     * 描述符开关变化
     *
     * @param tag            请求标识
     * @param device         设备
     * @param serviceUuid    服务UUID
     * @param characUuid     特征UUID
     * @param descriptorUuid 描述符UUID
     * @param isEnabled      开启或关闭
     */
    void onIndicationChanged(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid,
                             @NonNull UUID descriptorUuid, boolean isEnabled);

    /**
     * 最大传输单元变化
     *
     * @param tag    请求标识
     * @param device 设备
     * @param mtu    最大传输单元新的值
     */
    void onMtuChanged(@Nullable String tag, @NonNull Device device, @IntRange(from = 23, to = 517) int mtu);

    /**
     * @param tag    请求标识
     * @param device 设备
     */
    void onPhyRead(@Nullable String tag, @NonNull Device device, int txPhy, int rxPhy);

    /**
     * @param tag    请求标识
     * @param device 设备
     */
    void onPhyUpdate(@Nullable String tag, @NonNull Device device, int txPhy, int rxPhy);

    /**
     * 请求失败
     *
     * @param device   设备
     * @param request  请求实例
     * @param failType 失败类型。{@link Connection#REQUEST_FAIL_TYPE_GATT_IS_NULL}等
     */
    void onRequestFailed(@NonNull Device device, @NonNull Request request, int failType);

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

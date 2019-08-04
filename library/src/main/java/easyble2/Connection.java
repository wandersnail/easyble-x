package easyble2;

import android.bluetooth.*;
import android.os.Build;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import easyble2.callback.*;

import java.util.UUID;

/**
 * date: 2019/8/3 13:45
 * author: zengfansheng
 */
public interface Connection {
    UUID clientCharacteristicConfig = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * 普通请求失败
     */
    int REQUEST_FAIL_TYPE_REQUEST_FAILED = 0;
    int REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC = 1;
    int REQUEST_FAIL_TYPE_NULL_DESCRIPTOR = 2;
    int REQUEST_FAIL_TYPE_NULL_SERVICE = 3;
    /**
     * 请求结果不是[BluetoothGatt.GATT_SUCCESS]
     */
    int REQUEST_FAIL_TYPE_GATT_STATUS_FAILED = 4;
    int REQUEST_FAIL_TYPE_GATT_IS_NULL = 5;
    int REQUEST_FAIL_TYPE_API_LEVEL_TOO_LOW = 6;
    int REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED = 7;
    int REQUEST_FAIL_TYPE_REQUEST_TIMEOUT = 8;
    int REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED = 9;
    int REQUEST_FAIL_TYPE_CONNECTION_RELEASED = 10;
    int REQUEST_FAIL_TYPE_VALUE_IS_NULL_OR_EMPTY = 11;

    //----------连接状态-------------  
    int STATE_DISCONNECTED = 0;
    int STATE_CONNECTING = 1;
    int STATE_SCANNING = 2;
    /**
     * 已连接，还未执行发现服务
     */
    int STATE_CONNECTED = 3;
    /**
     * 已连接，正在发现服务
     */
    int STATE_SERVICE_DISCOVERING = 4;
    /**
     * 已连接，成功发现服务
     */
    int STATE_SERVICE_DISCOVERED = 5;
    /**
     * 连接已释放
     */
    int STATE_RELEASED = 6;

    //----------连接超时类型---------
    int TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE = 0;
    /**
     * 搜索到设备，但是无法连接成功
     */
    int TIMEOUT_TYPE_CANNOT_CONNECT = 1;
    /**
     * 连接成功，但是无法发现蓝牙服务，即[BluetoothGattCallback.onServicesDiscovered]不回调
     */
    int TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES = 2;

    //-------------连接失败类型-------------------
    int CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION = 1;
    int CONNECT_FAIL_TYPE_UNCONNECTABLE = 2;

    /**
     * 设置收到特征通知数据回调，作用于有nofity属性的特征
     */
    void setCharacteristicChangedCallback(CharacteristicChangedCallback callback);
    
    @NonNull
    Device getDevice();
    
    /**
     * 重连
     */
    void reconnect();

    /**
     * 断开连接
     */
    void disconnect();

    /**
     * 清理内部缓存并强制刷新蓝牙设备的服务
     */
    void refresh();

    /**
     * 销毁连接
     */
    void release();

    /**
     * 销毁连接，不通知观察者
     */
    void releaseNoEvnet();

    /**
     * 获取连接状态
     */
    int getConnctionState();

    /**
     * 是否开启了自动连接
     */
    boolean isAutoReconnectEnabled();

    @Nullable
    BluetoothGatt getGatt();

    /**
     * 清除请求队列，不触发事件
     */
    void clearRequestQueue();

    /**
     * 将指定的请求类型从队列中移除，不触发事件
     */
    void clearRequestQueueByType(@NonNull Request.RequestType type);

    @NonNull
    ConnectionConfiguration getConnectionConfiguration();

    @Nullable
    BluetoothGattService getService(@NonNull UUID serviceUuid);

    @Nullable
    BluetoothGattCharacteristic getCharacteristic(@NonNull UUID serviceUuid, @NonNull UUID characteristicUuid);

    @Nullable
    BluetoothGattDescriptor getDescriptor(@NonNull UUID serviceUuid, @NonNull UUID characteristicUuid, @NonNull UUID descriptorUuid);

    /**
     * 修改最大传输单元，使用观察者模式返回结果
     *
     * @param tag 用于区分请求，自定义
     * @param mtu 要修改成的值
     */
    void changeMtu(@Nullable String tag, @IntRange(from = 23, to = 517) int mtu);

    /**
     * 修改最大传输单元，使用接口回调返回结果
     *
     * @param tag 用于区分请求，自定义
     * @param mtu 要修改成的值
     */
    void changeMtu(@Nullable String tag, @IntRange(from = 23, to = 517) int mtu, @NonNull MtuChangedCallback callback);

    /**
     * 修改最大传输单元，使用观察者模式返回结果
     *
     * @param tag      用于区分请求，自定义
     * @param mtu      要修改成的值
     * @param priority 请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void changeMtu(@Nullable String tag, @IntRange(from = 23, to = 517) int mtu, int priority);

    /**
     * 修改最大传输单元，使用接口回调返回结果
     *
     * @param tag      用于区分请求，自定义
     * @param mtu      要修改成的值
     * @param priority 请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void changeMtu(@Nullable String tag, @IntRange(from = 23, to = 517) int mtu, int priority, @NonNull MtuChangedCallback callback);

    /**
     * 读取已连接的蓝牙设备的信号强度，使用观察者模式返回结果
     *
     * @param tag 用于区分请求，自定义
     */
    void readRssi(@Nullable String tag);

    /**
     * 读取已连接的蓝牙设备的信号强度，使用接口回调返回结果
     *
     * @param tag 用于区分请求，自定义
     */
    void readRssi(@Nullable String tag, @NonNull RemoteRssiReadCallback callback);

    /**
     * 读取已连接的蓝牙设备的信号强度，使用观察者模式返回结果
     *
     * @param tag      用于区分请求，自定义
     * @param priority 请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void readRssi(@Nullable String tag, int priority);

    /**
     * 读取已连接的蓝牙设备的信号强度，使用接口回调返回结果
     *
     * @param tag      用于区分请求，自定义
     * @param priority 请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void readRssi(@Nullable String tag, int priority, @NonNull RemoteRssiReadCallback callback);

    /**
     * 读取物理层发送器和接收器，使用观察者模式返回结果
     *
     * @param tag 用于区分请求，自定义
     */
    @RequiresApi(Build.VERSION_CODES.O)
    void readPhy(@Nullable String tag);

    /**
     * 读取物理层发送器和接收器，使用接口回调返回结果
     *
     * @param tag 用于区分请求，自定义
     */
    @RequiresApi(Build.VERSION_CODES.O)
    void readPhy(@Nullable String tag, @NonNull PhyReadCallback callback);

    /**
     * 读取物理层发送器和接收器，使用观察者模式返回结果
     *
     * @param tag      用于区分请求，自定义
     * @param priority 请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    @RequiresApi(Build.VERSION_CODES.O)
    void readPhy(@Nullable String tag, int priority);

    /**
     * 读取物理层发送器和接收器，使用接口回调返回结果
     *
     * @param tag      用于区分请求，自定义
     * @param priority 请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    @RequiresApi(Build.VERSION_CODES.O)
    void readPhy(@Nullable String tag, int priority, @NonNull PhyReadCallback callback);

    /**
     * 设置物理层发送器和接收器偏好，使用接口回调返回结果
     *
     * @param tag        用于区分请求，自定义
     * @param txPhy      物理层发送器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param rxPhy      物理层接收器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param phyOptions 物理层BLE首选传输编码。{@link BluetoothDevice#PHY_OPTION_NO_PREFERRED}等
     */
    @RequiresApi(Build.VERSION_CODES.O)
    void setPreferredPhy(@Nullable String tag, int txPhy, int rxPhy, int phyOptions);

    /**
     * 设置物理层发送器和接收器偏好，使用接口回调返回结果
     *
     * @param tag        用于区分请求，自定义
     * @param txPhy      物理层发送器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param rxPhy      物理层接收器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param phyOptions 物理层BLE首选传输编码。{@link BluetoothDevice#PHY_OPTION_NO_PREFERRED}等
     */
    @RequiresApi(Build.VERSION_CODES.O)
    void setPreferredPhy(@Nullable String tag, int txPhy, int rxPhy, int phyOptions, @NonNull PhyUpdateCallback callback);

    /**
     * 设置物理层发送器和接收器偏好，使用接口回调返回结果
     *
     * @param tag        用于区分请求，自定义
     * @param txPhy      物理层发送器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param rxPhy      物理层接收器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param phyOptions 物理层BLE首选传输编码。{@link BluetoothDevice#PHY_OPTION_NO_PREFERRED}等
     * @param priority   请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    @RequiresApi(Build.VERSION_CODES.O)
    void setPreferredPhy(@Nullable String tag, int txPhy, int rxPhy, int phyOptions, int priority);

    /**
     * 设置物理层发送器和接收器偏好，使用接口回调返回结果
     *
     * @param tag        用于区分请求，自定义
     * @param txPhy      物理层发送器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param rxPhy      物理层接收器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param phyOptions 物理层BLE首选传输编码。{@link BluetoothDevice#PHY_OPTION_NO_PREFERRED}等
     * @param priority   请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    @RequiresApi(Build.VERSION_CODES.O)
    void setPreferredPhy(@Nullable String tag, int txPhy, int rxPhy, int phyOptions, int priority, @NonNull PhyUpdateCallback callback);

    /**
     * 读取蓝牙设备的特征，使用观察者模式返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     */
    void readCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic);

    /**
     * 读取蓝牙设备的特征，使用观察者模式返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param priority       请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void readCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, int priority);

    /**
     * 读取蓝牙设备的特征，使用接口回调返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     */
    void readCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull CharacteristicReadCallback callback);

    /**
     * 读取蓝牙设备的特征，使用接口回调返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param priority       请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void readCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, int priority, @NonNull CharacteristicReadCallback callback);

    /**
     * 向特征写入，使用观察者模式返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param value          要写入的值
     */
    void writeCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull byte[] value);

    /**
     * 向特征写入，使用观察者模式返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param value          要写入的值
     * @param priority       请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void writeCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull byte[] value, int priority);

    /**
     * 向特征写入，使用接口回调返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param value          要写入的值
     */
    void writeCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull byte[] value, @NonNull CharacteristicWriteCallback callback);

    /**
     * 向特征写入，使用接口回调返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param value          要写入的值
     * @param priority       请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void writeCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull byte[] value, int priority, @NonNull CharacteristicWriteCallback callback);

    /**
     * 开关数据通知，使用观察者模式返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     */
    void setNotificationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled);

    /**
     * 开关数据通知，使用观察者模式返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param priority       请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void setNotificationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled, int priority);

    /**
     * 开关数据通知，使用接口回调返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     */
    void setNotificationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled, @NonNull NotificationChangedCallback callback);

    /**
     * 开关数据通知，使用接口回调返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param priority       请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void setNotificationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled, int priority, @NonNull NotificationChangedCallback callback);

    /**
     * 开关Indication，使用观察者模式返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     */
    void setIndicationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled);

    /**
     * 开关Indication，使用观察者模式返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param priority       请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void setIndicationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled, int priority);

    /**
     * 开关Indication，使用接口回调返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     */
    void setIndicationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled, @NonNull IndicationChangedCallback callback);

    /**
     * 开关Indication，使用接口回调返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param priority       请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void setIndicationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled, int priority, @NonNull IndicationChangedCallback callback);

    /**
     * 读取描述符的值，使用观察者模式返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param descriptor     描述符UUID
     */
    void readDescriptor(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull UUID descriptor);

    /**
     * 读取描述符的值，使用观察者模式返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param descriptor     描述符UUID
     * @param priority       请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void readDescriptor(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull UUID descriptor, int priority);

    /**
     * 读取描述符的值，使用接口回调返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param descriptor     描述符UUID
     */
    void readDescriptor(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull UUID descriptor, @NonNull DescriptorReadCallback callback);

    /**
     * 读取描述符的值，使用接口回调返回结果
     *
     * @param tag            用于区分请求，自定义
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param descriptor     描述符UUID
     * @param priority       请求优先级，值越大，优先级越高，用于请求队列中插队
     */
    void readDescriptor(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull UUID descriptor, int priority, @NonNull DescriptorReadCallback callback);

    /**
     * 通知或Indication是否开启
     */
    boolean isNotificationOrIndicationEnabled(@NonNull BluetoothGattCharacteristic characteristic);

    /**
     * 通知或Indication是否开启
     */
    boolean isNotificationOrIndicationEnabled(@NonNull UUID service, @NonNull UUID characteristic);
}

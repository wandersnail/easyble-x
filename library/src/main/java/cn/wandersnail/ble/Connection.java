package cn.wandersnail.ble;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    int REQUEST_FAIL_TYPE_CHARACTERISTIC_NOT_EXIST = 1;
    int REQUEST_FAIL_TYPE_DESCRIPTOR_NOT_EXIST = 2;
    int REQUEST_FAIL_TYPE_SERVICE_NOT_EXIST = 3;
    /**
     * 请求结果不是[BluetoothGatt.GATT_SUCCESS]
     */
    int REQUEST_FAIL_TYPE_GATT_STATUS_FAILED = 4;
    int REQUEST_FAIL_TYPE_GATT_IS_NULL = 5;
    int REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED = 6;
    int REQUEST_FAIL_TYPE_REQUEST_TIMEOUT = 7;
    int REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED = 8;
    int REQUEST_FAIL_TYPE_CONNECTION_RELEASED = 9;

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
    /**
     * 达到最大重连次数限制
     */
    int CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION = 1;
    /**
     * 不支持连接
     */
    int CONNECT_FAIL_TYPE_CONNECTION_IS_UNSUPPORTED = 2;
    /**
     * 缺少连接权限。 {@link Manifest.permission#BLUETOOTH_CONNECT}
     */
    int CONNECT_FAIL_TYPE_LACK_CONNECT_PERMISSION = 3;

    @NonNull
    Device getDevice();

    /**
     * 获取当前设置的最大传输单元
     */
    int getMtu();

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
    void releaseNoEvent();

    /**
     * 获取连接状态
     */
    @NonNull
    ConnectionState getConnectionState();

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
     * 将指定的请求类型从队列中移除，如果传null，则清除请求队列，不触发事件
     */
    void clearRequestQueueByType(@Nullable RequestType type);

    @NonNull
    ConnectionConfiguration getConnectionConfiguration();

    @Nullable
    BluetoothGattService getService(UUID service);

    @Nullable
    BluetoothGattCharacteristic getCharacteristic(UUID service, UUID characteristic);

    @Nullable
    BluetoothGattDescriptor getDescriptor(UUID service, UUID characteristic, UUID descriptor);

    /**
     * 执行请求
     */
    void execute(@NonNull Request request);

    /**
     * 通知或Indication是否开启
     */
    boolean isNotificationOrIndicationEnabled(@NonNull BluetoothGattCharacteristic characteristic);

    /**
     * 通知或Indication是否开启
     */
    boolean isNotificationOrIndicationEnabled(UUID service, UUID characteristic);

    /**
     * 设置原生回调
     */
    void setBluetoothGattCallback(BluetoothGattCallback callback);

    /**
     * 判断特征是否具有某属性
     *
     * @param service        特征所在服务的UUID
     * @param characteristic 特征的UUID
     * @param property       需要判断是否存在的属性。{@link BluetoothGattCharacteristic#PROPERTY_WRITE}等
     */
    boolean hasProperty(UUID service, UUID characteristic, int property);
}

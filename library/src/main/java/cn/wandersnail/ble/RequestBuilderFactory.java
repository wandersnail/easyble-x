package cn.wandersnail.ble;

import android.bluetooth.BluetoothDevice;
import android.os.Build;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.UUID;

import cn.wandersnail.ble.callback.MtuChangeCallback;
import cn.wandersnail.ble.callback.NotificationChangeCallback;
import cn.wandersnail.ble.callback.PhyChangeCallback;
import cn.wandersnail.ble.callback.ReadCharacteristicCallback;
import cn.wandersnail.ble.callback.ReadRssiCallback;

/**
 * date: 2019/9/20 18:06
 * author: zengfansheng
 */
public class RequestBuilderFactory {
    /**
     * 获取修改最大传输单元请求构建器
     *
     * @param mtu 要修改成的值
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public RequestBuilder<MtuChangeCallback> getChangeMtuBuilder(@IntRange(from = 23, to = 517) int mtu) {
        if (mtu < 23) {
            mtu = 23;
        } else if (mtu > 517) {
            mtu = 517;
        }
        RequestBuilder<MtuChangeCallback> builder = new RequestBuilder<>(RequestType.CHANGE_MTU);
        builder.value = mtu;
        return builder;
    }

    /**
     * 获取读取蓝牙设备的特征请求构建器
     *
     * @param service        服务UUID
     * @param characteristic 特征UUID
     */
    public RequestBuilder<ReadCharacteristicCallback> getReadCharacteristicBuilder(@NonNull UUID service, @NonNull UUID characteristic) {
        RequestBuilder<ReadCharacteristicCallback> builder = new RequestBuilder<>(RequestType.READ_CHARACTERISTIC);
        builder.service = service;
        builder.characteristic = characteristic;
        return builder;
    }

    /**
     * 获取开关数据通知请求构建器
     *
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param enable         开启或关闭
     */
    public RequestBuilder<NotificationChangeCallback> getSetNotificationBuilder(@NonNull UUID service, @NonNull UUID characteristic,
                                                                                 boolean enable) {
        RequestBuilder<NotificationChangeCallback> builder = new RequestBuilder<>(RequestType.SET_NOTIFICATION);
        builder.service = service;
        builder.characteristic = characteristic;
        builder.value = enable ? 1 : 0;
        return builder;
    }

    /**
     * 获取开关Indication请求构建器
     *
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param enable         开启或关闭
     */
    public RequestBuilder<NotificationChangeCallback> getSetIndicationBuilder(@NonNull UUID service, @NonNull UUID characteristic,
                                                                               boolean enable) {
        RequestBuilder<NotificationChangeCallback> builder = new RequestBuilder<>(RequestType.SET_INDICATION);
        builder.service = service;
        builder.characteristic = characteristic;
        builder.value = enable ? 1 : 0;
        return builder;
    }

    /**
     * 获取读取描述符的值请求构建器
     *
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param descriptor     描述符UUID
     */
    public RequestBuilder<NotificationChangeCallback> getReadDescriptorBuilder(@NonNull UUID service, @NonNull UUID characteristic,
                                                                                @NonNull UUID descriptor) {
        RequestBuilder<NotificationChangeCallback> builder = new RequestBuilder<>(RequestType.READ_DESCRIPTOR);
        builder.service = service;
        builder.characteristic = characteristic;
        builder.descriptor = descriptor;
        return builder;
    }

    /**
     * 获取向特征写入请求构建器
     *
     * @param service        服务UUID
     * @param characteristic 特征UUID
     * @param value          要写入特征的值
     */
    public WriteCharacteristicBuilder getWriteCharacteristicBuilder(@NonNull UUID service, @NonNull UUID characteristic,
                                                                            @NonNull byte[] value) {
        Inspector.requireNonNull(value, "value can't be null");
        WriteCharacteristicBuilder builder = new WriteCharacteristicBuilder();
        builder.service = service;
        builder.characteristic = characteristic;
        builder.value = value;
        return builder;
    }

    /**
     * 获取读取已连接的蓝牙设备的信号强度请求构建器
     */
    public RequestBuilder<ReadRssiCallback> getReadRssiBuilder() {
        return new RequestBuilder<>(RequestType.READ_RSSI);
    }

    /**
     * 获取读取物理层发送器和接收器请求构建器
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public RequestBuilder<PhyChangeCallback> getReadPhyBuilder() {
        return new RequestBuilder<>(RequestType.READ_PHY);
    }

    /**
     * 获取设置物理层发送器和接收器偏好请求构建器
     *
     * @param txPhy      物理层发送器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param rxPhy      物理层接收器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param phyOptions 物理层BLE首选传输编码。{@link BluetoothDevice#PHY_OPTION_NO_PREFERRED}等
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public RequestBuilder<PhyChangeCallback> getSetPreferredPhyBuilder(int txPhy, int rxPhy, int phyOptions) {
        RequestBuilder<PhyChangeCallback> builder = new RequestBuilder<>(RequestType.SET_PREFERRED_PHY);
        builder.value = new int[]{txPhy, rxPhy, phyOptions};
        return builder;
    }
}

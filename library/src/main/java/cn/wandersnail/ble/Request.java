package cn.wandersnail.ble;

import android.bluetooth.BluetoothDevice;
import android.os.Build;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.UUID;

import cn.wandersnail.ble.callback.MtuChangeCallback;
import cn.wandersnail.ble.callback.NotificationChangeCallback;
import cn.wandersnail.ble.callback.PhyChangeCallback;
import cn.wandersnail.ble.callback.ReadCharacteristicCallback;
import cn.wandersnail.ble.callback.ReadRssiCallback;
import cn.wandersnail.ble.callback.RequestCallback;
import cn.wandersnail.ble.callback.WriteCharacteristicCallback;
import cn.wandersnail.commons.observer.Observe;
import cn.wandersnail.commons.poster.RunOn;

/**
 * date: 2019/8/11 15:34
 * author: zengfansheng
 */
public interface Request {
    /**
     * 设备
     */
    @NonNull
    Device getDevice();

    /**
     * 请求类型
     */
    @NonNull
    RequestType getType();

    /**
     * 请求标识
     */
    @Nullable
    String getTag();

    /**
     * 服务UUID
     */
    @Nullable
    UUID getService();

    /**
     * 特征UUID
     */
    @Nullable
    UUID getCharacteristic();

    /**
     * 描述符UUID
     */
    @Nullable
    UUID getDescriptor();

    /**
     * 执行请求
     *
     * @param connection 请求执行的连接
     */
    void execute(Connection connection);

    class Builder<T extends RequestCallback> {
        String tag;
        RequestType type;
        UUID service;
        UUID characteristic;
        UUID descriptor;
        Object value;
        int priority;
        RequestCallback callback;
        WriteOptions writeOptions;

        private Builder(RequestType type) {
            this.type = type;
        }

        /**
         * @param tag 请求标识，用于标识每次请求，规则自定。可以用来区分相同类型的不同批次请求
         */
        public Builder<T> setTag(String tag) {
            this.tag = tag;
            return this;
        }

        /**
         * @param priority 请求优先级，值越大，优先级越高，用于请求队列中插队
         */
        public Builder<T> setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * 如果设置了回调，则观察者不会收到此次请求的消息；不设置则使用观察者接收请求结果。
         * <br>回调方法使用{@link RunOn}注解指定执行线程，观察者方法使用{@link Observe}注解指定执行线程
         */
        public Builder<T> setCallback(T callback) {
            this.callback = callback;
            return this;
        }

        public Request build() {
            return new GenericRequest(this);
        }
    }

    final class WriteCharacteristicBuilder extends Builder<WriteCharacteristicCallback> {
        private WriteCharacteristicBuilder() {
            super(RequestType.WRITE_CHARACTERISTIC);
        }

        @Override
        public WriteCharacteristicBuilder setTag(String tag) {
            super.setTag(tag);
            return this;
        }

        @Override
        public WriteCharacteristicBuilder setPriority(int priority) {
            super.setPriority(priority);
            return this;
        }

        @Override
        public WriteCharacteristicBuilder setCallback(WriteCharacteristicCallback callback) {
            super.setCallback(callback);
            return this;
        }

        /**
         * 设置此次请求的写入设置
         */
        public WriteCharacteristicBuilder setWriteOptions(WriteOptions writeOptions) {
            this.writeOptions = writeOptions;
            return this;
        }
    }

    /**
     * 获取修改最大传输单元请求构建器
     *
     * @param mtu 要修改成的值
     */
    static Builder<MtuChangeCallback> getChangeMtuBuilder(@IntRange(from = 23, to = 517) int mtu) {
        if (mtu < 23) {
            mtu = 23;
        } else if (mtu > 517) {
            mtu = 517;
        }
        Builder<MtuChangeCallback> builder = new Builder<>(RequestType.CHANGE_MTU);
        builder.value = mtu;
        return builder;
    }

    /**
     * 获取读取蓝牙设备的特征请求构建器
     *
     * @param service        服务UUID
     * @param characteristic 特征UUID
     */
    static Builder<ReadCharacteristicCallback> getReadCharacteristicBuilder(@NonNull UUID service, @NonNull UUID characteristic) {
        Builder<ReadCharacteristicCallback> builder = new Builder<>(RequestType.READ_CHARACTERISTIC);
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
    static Builder<NotificationChangeCallback> getSetNotificationBuilder(@NonNull UUID service, @NonNull UUID characteristic,
                                                                         boolean enable) {
        Builder<NotificationChangeCallback> builder = new Builder<>(RequestType.SET_NOTIFICATION);
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
    static Builder<NotificationChangeCallback> getSetIndicationBuilder(@NonNull UUID service, @NonNull UUID characteristic,
                                                                       boolean enable) {
        Builder<NotificationChangeCallback> builder = new Builder<>(RequestType.SET_INDICATION);
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
    static Builder<NotificationChangeCallback> getReadDescriptorBuilder(@NonNull UUID service, @NonNull UUID characteristic,
                                                                        @NonNull UUID descriptor) {
        Builder<NotificationChangeCallback> builder = new Builder<>(RequestType.READ_DESCRIPTOR);
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
    static WriteCharacteristicBuilder getWriteCharacteristicBuilder(@NonNull UUID service, @NonNull UUID characteristic,
                                                                    @NonNull byte[] value) {
        Inspector.requireNonNull(value, "value can't be");
        WriteCharacteristicBuilder builder = new WriteCharacteristicBuilder();
        builder.service = service;
        builder.characteristic = characteristic;
        builder.value = value;
        return builder;
    }

    /**
     * 获取读取已连接的蓝牙设备的信号强度请求构建器
     */
    static Builder<ReadRssiCallback> getReadRssiBuilder() {
        return new Builder<>(RequestType.READ_RSSI);
    }

    /**
     * 获取读取物理层发送器和接收器请求构建器
     */
    @RequiresApi(Build.VERSION_CODES.O)
    static Builder<PhyChangeCallback> getReadPhyBuilder() {
        return new Builder<>(RequestType.READ_PHY);
    }

    /**
     * 获取设置物理层发送器和接收器偏好请求构建器
     *
     * @param txPhy      物理层发送器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param rxPhy      物理层接收器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param phyOptions 物理层BLE首选传输编码。{@link BluetoothDevice#PHY_OPTION_NO_PREFERRED}等
     */
    @RequiresApi(Build.VERSION_CODES.O)
    static Builder<PhyChangeCallback> getSetPreferredPhyBuilder(int txPhy, int rxPhy, int phyOptions) {
        Builder<PhyChangeCallback> builder = new Builder<>(RequestType.SET_PREFERRED_PHY);
        builder.value = new int[]{txPhy, rxPhy, phyOptions};
        return builder;
    }
}

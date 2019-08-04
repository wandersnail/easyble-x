package easyble2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

/**
 * date: 2019/8/4 08:14
 * author: zengfansheng
 */
class MethodInfoGenerator {
    static MethodInfo onBluetoothAdapterStateChanged(int state) {
        return new MethodInfo("onBluetoothAdapterStateChanged", new TypeValuePair(int.class, state));
    }

    static MethodInfo onConnectionStateChanged(@NonNull Device device) {
        return new MethodInfo("onConnectionStateChanged", new TypeValuePair(Device.class, device));
    }

    static MethodInfo onConnectFailed(@NonNull Device device, int failType) {
        return new MethodInfo("onConnectFailed", new TypeValuePair(Device.class, device), new TypeValuePair(int.class, failType));
    }

    static MethodInfo onConnectTimeout(@NonNull Device device, int type) {
        return new MethodInfo("onConnectTimeout", new TypeValuePair(Device.class, device), new TypeValuePair(int.class, type));
    }
    
    static MethodInfo onCharacteristicChanged(@NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value) {
        return new MethodInfo("onCharacteristicChanged", new TypeValuePair(Device.class, device), new TypeValuePair(UUID.class, serviceUuid),
                new TypeValuePair(UUID.class, characUuid), new TypeValuePair(byte[].class, value));
    }

    static MethodInfo onCharacteristicRead(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value) {
        return new MethodInfo("onCharacteristicRead", new TypeValuePair(String.class, tag), new TypeValuePair(Device.class, device),
                new TypeValuePair(UUID.class, serviceUuid), new TypeValuePair(UUID.class, characUuid), new TypeValuePair(byte[].class, value));
    }

    static MethodInfo onCharacteristicWrite(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value) {
        return new MethodInfo("onCharacteristicWrite", new TypeValuePair(String.class, tag), new TypeValuePair(Device.class, device),
                new TypeValuePair(UUID.class, serviceUuid), new TypeValuePair(UUID.class, characUuid), new TypeValuePair(byte[].class, value));
    }

    static MethodInfo onRemoteRssiRead(@Nullable String tag, @NonNull Device device, int ssid) {
        return new MethodInfo("onRemoteRssiRead", new TypeValuePair(String.class, tag), new TypeValuePair(Device.class, device),
                new TypeValuePair(int.class, ssid));
    }

    static MethodInfo onDescriptorRead(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid,
                                                  @NonNull UUID descriptorUuid, @NonNull byte[] value) {
        return new MethodInfo("onDescriptorRead", new TypeValuePair(String.class, tag), new TypeValuePair(Device.class, device),
                new TypeValuePair(UUID.class, serviceUuid), new TypeValuePair(UUID.class, characUuid), new TypeValuePair(UUID.class, descriptorUuid),
                new TypeValuePair(byte[].class, value));
    }

    static MethodInfo onNotificationChanged(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid,
                                                       @NonNull UUID descriptorUuid, boolean isEnabled) {
        return new MethodInfo("onNotificationChanged", new TypeValuePair(String.class, tag), new TypeValuePair(Device.class, device),
                new TypeValuePair(UUID.class, serviceUuid), new TypeValuePair(UUID.class, characUuid), new TypeValuePair(UUID.class, descriptorUuid),
                new TypeValuePair(boolean.class, isEnabled));
    }

    static MethodInfo onIndicationChanged(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid,
                                                     @NonNull UUID descriptorUuid, boolean isEnabled) {
        return new MethodInfo("onIndicationChanged", new TypeValuePair(String.class, tag), new TypeValuePair(Device.class, device),
                new TypeValuePair(UUID.class, serviceUuid), new TypeValuePair(UUID.class, characUuid), new TypeValuePair(UUID.class, descriptorUuid),
                new TypeValuePair(boolean.class, isEnabled));
    }

    static MethodInfo onMtuChanged(@Nullable String tag, @NonNull Device device, int mtu) {
        return new MethodInfo("onMtuChanged", new TypeValuePair(String.class, tag), new TypeValuePair(Device.class, device),
                new TypeValuePair(int.class, mtu));
    }

    static MethodInfo onPhyRead(@Nullable String tag, @NonNull Device device, int txPhy, int rxPhy) {
        return new MethodInfo("onPhyRead", new TypeValuePair(String.class, tag), new TypeValuePair(Device.class, device),
                new TypeValuePair(int.class, txPhy), new TypeValuePair(int.class, rxPhy));
    }

    static MethodInfo onPhyUpdate(@Nullable String tag, @NonNull Device device, int txPhy, int rxPhy) {
        return new MethodInfo("onPhyUpdate", new TypeValuePair(String.class, tag), new TypeValuePair(Device.class, device),
                new TypeValuePair(int.class, txPhy), new TypeValuePair(int.class, rxPhy));
    }

    static MethodInfo onRequestFailed(@NonNull Device device, @NonNull Request request, int failType) {
        return new MethodInfo("onRequestFailed", new TypeValuePair(Device.class, device), new TypeValuePair(Request.class, request),
                new TypeValuePair(int.class, failType));
    }
}

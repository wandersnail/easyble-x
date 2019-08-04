package easyble2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

/**
 * date: 2019/8/3 16:02
 * author: zengfansheng
 */
public class SimpleEventObserver implements EventObserver {
    @Override
    public void onBluetoothAdapterStateChanged(int state) {
        
    }

    @Override
    public void onCharacteristicRead(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value) {

    }

    @Override
    public void onCharacteristicChanged(@NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value) {

    }

    @Override
    public void onCharacteristicWrite(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value) {

    }

    @Override
    public void onRemoteRssiRead(@Nullable String tag, @NonNull Device device, int ssid) {

    }

    @Override
    public void onDescriptorRead(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull UUID descriptorUuid, @NonNull byte[] value) {

    }

    @Override
    public void onNotificationChanged(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull UUID descriptorUuid, boolean isEnabled) {

    }

    @Override
    public void onIndicationChanged(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull UUID descriptorUuid, boolean isEnabled) {

    }

    @Override
    public void onMtuChanged(@Nullable String tag, @NonNull Device device, int mtu) {

    }

    @Override
    public void onPhyRead(@Nullable String tag, @NonNull Device device, int txPhy, int rxPhy) {

    }

    @Override
    public void onPhyUpdate(@Nullable String tag, @NonNull Device device, int txPhy, int rxPhy) {

    }

    @Override
    public void onRequestFailed(@NonNull Device device, @NonNull Request request, int failType) {

    }

    @Override
    public void onConnectionStateChanged(@NonNull Device device) {

    }

    @Override
    public void onConnectFailed(@NonNull Device device, int failType) {

    }

    @Override
    public void onConnectTimeout(@NonNull Device device, int type) {

    }
}

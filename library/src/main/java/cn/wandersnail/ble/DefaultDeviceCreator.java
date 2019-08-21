package cn.wandersnail.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * date: 2019/8/3 13:03
 * author: zengfansheng
 */
class DefaultDeviceCreator implements DeviceCreator {
    @Nullable
    @Override
    public Device create(@NonNull BluetoothDevice device, @Nullable ScanResult scanResult) {
        return new Device(device);
    }
}

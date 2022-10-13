package cn.wandersnail.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

/**
 * date: 2019/10/1 15:13
 * author: zengfansheng
 */
class LegacyScanner extends AbstractScanner implements BluetoothAdapter.LeScanCallback {
    
    LegacyScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
        super(easyBle, bluetoothAdapter);
    }

    @Override
    protected boolean isReady() {
        return true;
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void performStartScan() {
        bluetoothAdapter.startLeScan(this);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void performStopScan() {
        bluetoothAdapter.stopLeScan(this);
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {        
        parseScanResult(device, false, null, rssi, scanRecord);
    }

    @NonNull
    @Override
    public ScannerType getType() {
        return ScannerType.LEGACY;
    }
}

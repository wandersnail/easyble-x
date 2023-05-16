package cn.wandersnail.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import androidx.annotation.NonNull;

import cn.wandersnail.ble.callback.ScanListener;
import cn.wandersnail.ble.util.Logger;

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
        try {
            boolean b = bluetoothAdapter.startLeScan(this);
            if (!b) {
                handleErrorAndStop(ScanListener.ERROR_SCAN_FAILED, "start failed");
            }
        } catch (Exception e) {
            logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, "搜索开始失败：" + e.getMessage());
            handleErrorAndStop(ScanListener.ERROR_SCAN_FAILED, e.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void performStopScan() {
        try {
            bluetoothAdapter.stopLeScan(this);
        } catch (Exception e) {
            logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, "搜索结束失败：" + e.getMessage());
        }
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

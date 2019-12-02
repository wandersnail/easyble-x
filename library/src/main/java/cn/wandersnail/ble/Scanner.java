package cn.wandersnail.ble;

import android.content.Context;

import androidx.annotation.NonNull;

import cn.wandersnail.ble.callback.ScanListener;

/**
 * 蓝牙设备搜索器
 * 
 * date: 2019/10/1 14:41
 * author: zengfansheng
 */
interface Scanner {
    
    void addScanListener(@NonNull ScanListener listener);

    void removeScanListener(@NonNull ScanListener listener);

    void startScan(@NonNull Context context);

    void stopScan(boolean quietly);

    boolean isScanning();

    void onBluetoothOff();

    void release();
    
    @NonNull
    ScannerType getType();
}

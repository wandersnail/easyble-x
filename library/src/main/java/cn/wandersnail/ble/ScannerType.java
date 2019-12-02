package cn.wandersnail.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;

/**
 * date: 2019/12/2 11:51
 * author: zengfansheng
 */
public enum ScannerType {
    /**
     * 对应{@link BluetoothLeScanner}
     */
    LE,
    /**
     * 使用{@link BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)}
     */
    LEGACY
}

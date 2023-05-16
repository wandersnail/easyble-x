package cn.wandersnail.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import cn.wandersnail.ble.callback.ScanListener;
import cn.wandersnail.ble.util.Logger;

/**
 * 蓝牙搜索器
 * <p>
 * date: 2019/8/3 12:30
 * author: zengfansheng
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class LeScanner extends AbstractScanner {
    private BluetoothLeScanner bleScanner;

    LeScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
        super(easyBle, bluetoothAdapter);       
    }

    private BluetoothLeScanner getLeScanner() {
        if (bleScanner == null) {
            //如果蓝牙未开启的时候，获取到是null
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        return bleScanner;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            parseScanResult(result.getDevice(), result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            handleScanCallback(false, null, false, ScanListener.ERROR_SCAN_FAILED, "onScanFailed. errorCode = " + errorCode);
            logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, "onScanFailed. errorCode = " + errorCode);
            stopScan(true);
        }
    };

    @Override
    protected boolean isReady() {
        return getLeScanner() != null;
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void performStartScan() {
        ScanSettings settings;
        if (configuration.scanSettings == null) {
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build();
        } else {
            settings = configuration.scanSettings;
        }
        try {
            bleScanner.startScan(configuration.filters, settings, scanCallback);
        } catch (Exception e) {
            logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, "搜索开始失败：" + e.getMessage());
            handleErrorAndStop(ScanListener.ERROR_SCAN_FAILED, e.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void performStopScan() {
        if (bleScanner != null) {
            try {
                bleScanner.stopScan(scanCallback);
            } catch (Exception e) {
                logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, "搜索结束失败：" + e.getMessage());
            }
        }
    }

    @NonNull
    @Override
    public ScannerType getType() {
        return ScannerType.LE;
    }
}

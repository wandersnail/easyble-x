package cn.wandersnail.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import androidx.annotation.NonNull;

import cn.wandersnail.ble.callback.ScanListener;
import cn.wandersnail.ble.util.Logger;

/**
 * date: 2020/5/9 16:20
 * author: zengfansheng
 */
class ClassicScanner extends AbstractScanner {
    private boolean stopQuietly = false;
    
    ClassicScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
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
            boolean b = bluetoothAdapter.startDiscovery();
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
            bluetoothAdapter.cancelDiscovery();
        } catch (Exception e) {
            logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, "搜索结束失败：" + e.getMessage());
        }
    }

    @Override
    void setScanning(boolean scanning) {
        super.setScanning(scanning);
        if (scanning) {
            handleScanCallback(true, null, false, -1, "");
        } else if (!stopQuietly) {
            handleScanCallback(false, null, false, -1, "");
        } else {
            stopQuietly = false;
        }
    }

    @Override
    public void stopScan(boolean quietly) {
        if (isScanning()) {
            stopQuietly = quietly;
        }
        super.stopScan(quietly);
    }

    @NonNull
    @Override
    public ScannerType getType() {
        return ScannerType.CLASSIC;
    }
}

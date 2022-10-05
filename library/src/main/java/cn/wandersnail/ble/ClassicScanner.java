package cn.wandersnail.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;

import androidx.annotation.NonNull;

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
        bluetoothAdapter.startDiscovery();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void performStopScan() {
        bluetoothAdapter.cancelDiscovery();
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

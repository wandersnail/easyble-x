package cn.wandersnail.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cn.wandersnail.ble.callback.ScanListener;
import cn.wandersnail.ble.util.Logger;

/**
 * 蓝牙搜索器
 * <p>
 * date: 2019/8/3 12:30
 * author: zengfansheng
 */
class Scanner {
    private final ScanConfiguration configuration;
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler mainHandler;
    private boolean isScanning;
    private BluetoothLeScanner bleScanner;
    private final List<ScanListener> scanListeners = new ArrayList<>();
    private final SparseArray<BluetoothProfile> proxyBluetoothProfiles = new SparseArray<>();
    private final Logger logger;
    private final DeviceCreator deviceCreator;

    Scanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.configuration = easyBle.scanConfiguration;
        mainHandler = new Handler(Looper.getMainLooper());
        logger = easyBle.getLogger();
        deviceCreator = easyBle.getDeviceCreator();        
    }

    private BluetoothLeScanner getLeScanner() {
        if (bleScanner == null) {
            //如果蓝牙未开启的时候，获取到是null
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        return bleScanner;
    }
    
    synchronized void addScanListener(@NonNull ScanListener listener) {
        if (!scanListeners.contains(listener)) {
            scanListeners.add(listener);
        }
    }

    synchronized void removeScanListener(@NonNull ScanListener listener) {
        scanListeners.remove(listener);
    }

    //位置服务是否开户
    private boolean isLocationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return locationManager != null && locationManager.isLocationEnabled();
        } else {
            try {
                int locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
                return locationMode != Settings.Secure.LOCATION_MODE_OFF;
            } catch (Settings.SettingNotFoundException e) {
                return false;
            }
        }
    }

    //检查是否有定位权限
    private boolean noLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    //处理搜索回调
    private void handleScanCallback(final boolean start, @Nullable final Device device, final int errorCode, final String errorMsg) {
        mainHandler.post(() -> {
            for (ScanListener listener : scanListeners) {
                if (device != null) {
                    listener.onScanResult(device);
                } else if (start) {
                    listener.onScanStart();
                } else if (errorCode >= 0) {
                    listener.onScanError(errorCode, errorMsg);
                } else {
                    listener.onScanStop();
                }
            }
        });
    }

    //如果系统已配对连接，那么是无法搜索到的，所以尝试获取已连接的设备
    @SuppressWarnings("all")
    private void getSystemConnectedDevices(Context context) {
        try {
            Method method = bluetoothAdapter.getClass().getDeclaredMethod("getConnectionState");
            method.setAccessible(true);
            int state = (int) method.invoke(bluetoothAdapter);
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : devices) {
                    Method isConnectedMethod = device.getClass().getDeclaredMethod("isConnected");
                    isConnectedMethod.setAccessible(true);
                    boolean isConnected = (boolean) isConnectedMethod.invoke(device);
                    if (isConnected) {
                        parseScanResult(device, null);
                    }
                }
            }
        } catch (Exception ignore) {
        }
        //遍历支持的，获取所有连接的
        for (int i = 1; i <= 21; i++) {
            try {
                getSystemConnectedDevices(context, i);
            } catch (Exception ignore) {
            }
        }
    }

    private void getSystemConnectedDevices(Context context, int profile) {
        bluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (proxy == null) return;
                proxyBluetoothProfiles.put(profile, proxy);
                synchronized (Scanner.this) {
                    if (!isScanning) return;
                }
                try {
                    List<BluetoothDevice> devices = proxy.getConnectedDevices();
                    for (BluetoothDevice device : devices) {
                        parseScanResult(device, null);
                    }
                } catch (Exception ignore) {
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {

            }
        }, profile);
    }

    private void parseScanResult(BluetoothDevice device, @Nullable ScanResult result) {
        if ((configuration.onlyAcceptBleDevice && device.getType() != BluetoothDevice.DEVICE_TYPE_LE) ||
                !device.getAddress().matches("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$")) {
            return;
        }
        int rssi = result == null ? -120 : result.getRssi();
        String name = device.getName() == null ? "" : device.getName();
        if (configuration.rssiLowLimit <= rssi) {
            //通过构建器实例化Device
            Device dev = deviceCreator.create(device, result);
            if (dev != null) {
                dev.name = TextUtils.isEmpty(dev.getName()) ? name : dev.getName();
                dev.rssi = rssi;
                dev.scanResult = result;
                handleScanCallback(false, dev, -1, "");
            }
        }
        String msg = String.format(Locale.US, "found device! [name: %s, addr: %s]", name.isEmpty() ? "N/A" : name, device.getAddress());
        logger.log(Log.DEBUG, Logger.TYPE_SCAN_STATE, msg);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            parseScanResult(result.getDevice(), result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            handleScanCallback(false, null, ScanListener.ERROR_SCAN_FAILED, "onScanFailed. errorCode = " + errorCode);
            logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, "onScanFailed. errorCode = " + errorCode);
            stopScan(true);
        }
    };

    void startScan(Context context) {
        synchronized (this) {
            if (!bluetoothAdapter.isEnabled() || isScanning || getLeScanner() == null) {
                return;
            }
            if (!isLocationEnabled(context)) {
                String errorMsg = "Unable to scan for Bluetooth devices, the phone's location service is not turned on.";
                handleScanCallback(false, null, ScanListener.ERROR_LOCATION_SERVICE_CLOSED, errorMsg);
                logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, errorMsg);
                return;
            } else if (noLocationPermission(context)) {
                String errorMsg = "Unable to scan for Bluetooth devices, lack location permission.";
                handleScanCallback(false, null, ScanListener.ERROR_LACK_LOCATION_PERMISSION, errorMsg);
                logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, errorMsg);
                return;
            }
            isScanning = true;
        }
        handleScanCallback(true, null, -1, "");
        if (configuration.acceptSysConnectedDevice) {
            getSystemConnectedDevices(context);
        }
        bleScanner.startScan(configuration.filters, configuration.scanSettings, scanCallback);
        mainHandler.postDelayed(stopScanRunnable, configuration.scanPeriodMillis);
    }

    void stopScan(boolean quietly) {
        mainHandler.removeCallbacks(stopScanRunnable);
        int size = proxyBluetoothProfiles.size();
        for (int i = 0; i < size; i++) {
            try {
                bluetoothAdapter.closeProfileProxy(proxyBluetoothProfiles.keyAt(i), proxyBluetoothProfiles.valueAt(i));
            } catch (Exception ignore) {
            }
        }
        proxyBluetoothProfiles.clear();
        if (bleScanner != null) {
            bleScanner.stopScan(scanCallback);
        }
        if (!bluetoothAdapter.isEnabled()) return;
        if (isScanning) {
            isScanning = false;
            if (!quietly) {
                handleScanCallback(false, null, -1, "");
            }
        }
    }

    private Runnable stopScanRunnable = () -> stopScan(false);

    boolean isScanning() {
        return isScanning;
    }

    void onBluetoothOff() {
        isScanning = false;
        handleScanCallback(false, null, -1, "");
    }

    void release() {
        stopScan(false);
        scanListeners.clear();
    }
}

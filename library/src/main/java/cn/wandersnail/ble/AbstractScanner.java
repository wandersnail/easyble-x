package cn.wandersnail.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.wandersnail.ble.callback.ScanListener;
import cn.wandersnail.ble.util.Logger;

/**
 * date: 2019/10/1 14:44
 * author: zengfansheng
 */
abstract class AbstractScanner implements Scanner {
    final ScanConfiguration configuration;
    final BluetoothAdapter bluetoothAdapter;
    private final Handler mainHandler;
    private boolean isScanning;
    private final List<ScanListener> scanListeners = new CopyOnWriteArrayList<>();
    private final SparseArray<BluetoothProfile> proxyBluetoothProfiles = new SparseArray<>();
    final Logger logger;
    private final DeviceCreator deviceCreator;

    AbstractScanner(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.configuration = easyBle.scanConfiguration;
        mainHandler = new Handler(Looper.getMainLooper());
        logger = easyBle.getLogger();
        deviceCreator = easyBle.getDeviceCreator();
    }
    
    @Override
    public void addScanListener(@NonNull ScanListener listener) {
        if (!scanListeners.contains(listener)) {
            scanListeners.add(listener);
        }
    }

    @Override
    public void removeScanListener(@NonNull ScanListener listener) {
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
        int sdkVersion = context.getApplicationInfo().targetSdkVersion;
        if (sdkVersion >= Build.VERSION_CODES.Q) {//target sdk版本在29以上的需要精确定位权限才能搜索到蓝牙设备
            return !PermissionChecker.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            return !PermissionChecker.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) &&
                    !PermissionChecker.hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    //检查是否有搜索权限
    private boolean noScanPermission(Context context) {
        //在31以上的需要搜索权限才能搜索到蓝牙设备
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return !PermissionChecker.hasPermission(context, Manifest.permission.BLUETOOTH_SCAN);
        }
        return false;
    }
    
    //检查是否有连接权限，部分机型获取设备名称需要连接权限
    private boolean noConnectPermission(Context context) {
        //在31以上的需要搜索权限才能搜索到蓝牙设备
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return !PermissionChecker.hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT);
        }
        return false;
    }

    //处理搜索回调
    void handleScanCallback(final boolean start, @Nullable final Device device, final boolean isConnectedBySys,
                            final int errorCode, final String errorMsg) {
        mainHandler.post(() -> {
            for (ScanListener listener : scanListeners) {
                if (device != null) {
                    listener.onScanResult(device, isConnectedBySys);
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
                        parseScanResult(device, true);
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        //遍历支持的，获取所有连接的
        for (int i = 1; i <= 21; i++) {
            try {
                getSystemConnectedDevices(context, i);
            } catch (Throwable ignore) {
            }
        }
    }

    private void getSystemConnectedDevices(Context context, int profile) {
        bluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (proxy == null) return;
                proxyBluetoothProfiles.put(profile, proxy);
                synchronized (AbstractScanner.this) {
                    if (!isScanning) return;
                }
                try {
                    List<BluetoothDevice> devices = proxy.getConnectedDevices();
                    for (BluetoothDevice device : devices) {
                        parseScanResult(device, true);
                    }
                } catch (Throwable ignore) {
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {

            }
        }, profile);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    void parseScanResult(BluetoothDevice device, @Nullable ScanResult result) {
        if (result == null) {
            parseScanResult(device, false);
        } else {
            ScanRecord record = result.getScanRecord();
            parseScanResult(device, false, result, result.getRssi(), record == null ? null : record.getBytes());            
        }
    }

    private void parseScanResult(BluetoothDevice device, boolean isConnectedBySys) {
        parseScanResult(device, isConnectedBySys, null, -120, null);
    }
    
    void parseScanResult(BluetoothDevice device, boolean isConnectedBySys, @Nullable ScanResult result, int rssi, byte[] scanRecord) {
        if ((configuration.onlyAcceptBleDevice && device.getType() != BluetoothDevice.DEVICE_TYPE_LE) ||
                !device.getAddress().matches("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$")) {
            return;
        }
        String name = device.getName() == null ? "" : device.getName();
        if (configuration.rssiLowLimit <= rssi) {
            //通过构建器实例化Device
            Device dev = deviceCreator.create(device, result);
            if (dev != null) {
                dev.name = TextUtils.isEmpty(dev.getName()) ? name : dev.getName();
                dev.rssi = rssi;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    dev.scanResult = result;
                }
                dev.scanRecord = scanRecord;
                handleScanCallback(false, dev, isConnectedBySys, -1, "");
            }
        }
        String msg = String.format(Locale.US, "found device! [name: %s, addr: %s]", TextUtils.isEmpty(name) ? "N/A" : name, device.getAddress());
        logger.log(Log.DEBUG, Logger.TYPE_SCAN_STATE, msg);
    }

    @CallSuper
    @Override
    public void startScan(@NonNull Context context) {
        synchronized (this) {
            if (!isBtEnabled() || (getType() != ScannerType.CLASSIC && isScanning) || !isReady()) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isLocationEnabled(context)) {
                    String errorMsg = "Unable to scan for Bluetooth devices, the phone's location service is not turned on.";
                    handleScanCallback(false, null, false, ScanListener.ERROR_LOCATION_SERVICE_CLOSED, errorMsg);
                    logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, errorMsg);
                    return;
                }
                if (noLocationPermission(context)) {
                    String errorMsg = "Unable to scan for Bluetooth devices, lack location permission.";
                    handleScanCallback(false, null, false, ScanListener.ERROR_LACK_LOCATION_PERMISSION, errorMsg);
                    logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, errorMsg);
                    return;
                }
                if (noScanPermission(context)) {
                    String errorMsg = "Unable to scan for Bluetooth devices, lack scan permission.";
                    handleScanCallback(false, null, false, ScanListener.ERROR_LACK_SCAN_PERMISSION, errorMsg);
                    logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, errorMsg);
                    return;
                }
                if (noConnectPermission(context)) {
                    String errorMsg = "Unable to scan for Bluetooth devices, lack connect permission.";
                    handleScanCallback(false, null, false, ScanListener.ERROR_LACK_CONNECT_PERMISSION, errorMsg);
                    logger.log(Log.ERROR, Logger.TYPE_SCAN_STATE, errorMsg);
                    return;
                }
            }
            if (getType() != ScannerType.CLASSIC) {
                isScanning = true;
            }
        }
        if (getType() != ScannerType.CLASSIC) {
            handleScanCallback(true, null, false, -1, "");
        }
        if (configuration.acceptSysConnectedDevice) {
            getSystemConnectedDevices(context);
        }
        performStartScan();
        if (getType() != ScannerType.CLASSIC) {
            mainHandler.postDelayed(stopScanRunnable, configuration.scanPeriodMillis);
        }
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    @CallSuper
    void setScanning(boolean scanning) {
        synchronized (this) {
            isScanning = scanning;
        }
    }
    
    @CallSuper
    @Override
    public void stopScan(boolean quietly) {
        mainHandler.removeCallbacks(stopScanRunnable);
        int size = proxyBluetoothProfiles.size();
        for (int i = 0; i < size; i++) {
            try {
                bluetoothAdapter.closeProfileProxy(proxyBluetoothProfiles.keyAt(i), proxyBluetoothProfiles.valueAt(i));
            } catch (Exception ignore) {
            }
        }
        proxyBluetoothProfiles.clear();
        if (isBtEnabled()) {
            performStopScan();
        }
        if (getType() != ScannerType.CLASSIC) {
            synchronized (this) {
                if (isScanning) {
                    isScanning = false;
                    if (!quietly) {
                        handleScanCallback(false, null, false, -1, "");
                    }
                }
            }
        }
    }

    private final Runnable stopScanRunnable = () -> stopScan(false);

    //蓝牙是否开启
    private boolean isBtEnabled() {
        if (bluetoothAdapter.isEnabled()) {
            try {
                Method method = bluetoothAdapter.getClass().getDeclaredMethod("isLeEnabled");
                method.setAccessible(true);
                return (boolean) method.invoke(bluetoothAdapter);
            } catch (Throwable e) {
                int state = bluetoothAdapter.getState();
                return state == BluetoothAdapter.STATE_ON || state == 15;
            }
        }
        return false;
    }
    
    @Override
    public void onBluetoothOff() {
        synchronized (this) {
            isScanning = false;
        }
        handleScanCallback(false, null, false, -1, "");
    }

    @Override
    public void release() {
        stopScan(false);
        scanListeners.clear();
    }

    /**
     * 是否可搜索
     */
    protected abstract boolean isReady();
    
    /**
     * 执行搜索
     */
    protected abstract void performStartScan();

    /**
     * 执行停止搜索
     */
    protected abstract void performStopScan();
}

package cn.wandersnail.ble;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import cn.wandersnail.ble.callback.ScanListener;
import cn.wandersnail.ble.util.DefaultLogger;
import cn.wandersnail.ble.util.Logger;
import cn.wandersnail.commons.observer.Observable;
import cn.wandersnail.commons.poster.MethodInfo;
import cn.wandersnail.commons.poster.PosterDispatcher;

/**
 * date: 2019/8/3 11:50
 * author: zengfansheng
 */
public class EasyBLE {
    static volatile EasyBLE instance;
    private static final EasyBLEBuilder DEFAULT_BUILDER = new EasyBLEBuilder();
    private final ExecutorService executorService;
    private final PosterDispatcher posterDispatcher;
    private final BondController bondController;
    private final DeviceCreator deviceCreator;
    private final Observable observable;
    private final Logger logger;
    private final ScannerType scannerType;
    public final ScanConfiguration scanConfiguration;
    private Scanner scanner;
    private Application application;
    private boolean isInitialized;
    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver broadcastReceiver;
    private final Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    //已连接的设备MAC地址集合
    private final List<String> addressList = new CopyOnWriteArrayList<>();
    private final boolean internalObservable;

    private EasyBLE() {
        this(DEFAULT_BUILDER);
    }

    EasyBLE(EasyBLEBuilder builder) {
        tryGetApplication();
        bondController = builder.bondController;
        scannerType = builder.scannerType;
        deviceCreator = builder.deviceCreator == null ? new DefaultDeviceCreator() : builder.deviceCreator;
        scanConfiguration = builder.scanConfiguration == null ? new ScanConfiguration() : builder.scanConfiguration;
        logger = builder.logger == null ? new DefaultLogger("EasyBLE") : builder.logger;
        if (builder.observable != null) {
            internalObservable = false;
            observable = builder.observable;
            posterDispatcher = observable.getPosterDispatcher();
            executorService = posterDispatcher.getExecutorService();
        } else {
            internalObservable = true;
            executorService = builder.executorService;
            posterDispatcher = new PosterDispatcher(executorService, builder.methodDefaultThreadMode);
            observable = new Observable(posterDispatcher, builder.isObserveAnnotationRequired);
        }    
    }

    /**
     * 获取实例。单例的
     */
    public static EasyBLE getInstance() {
        if (instance == null) {
            synchronized (EasyBLE.class) {
                if (instance == null) {
                    instance = new EasyBLE();
                }
            }
        }
        return instance;
    }

    public static EasyBLEBuilder getBuilder() {
        return new EasyBLEBuilder();
    }

    @Nullable
    Context getContext() {
        if (application == null) {
            tryAutoInit();
        }
        return application;
    }

    @SuppressLint("PrivateApi")
    private void tryGetApplication() {
        try {
            Class<?> cls = Class.forName("android.app.ActivityThread");
            Method method = cls.getMethod("currentActivityThread");
            method.setAccessible(true);
            Object acThread = method.invoke(null);
            Method appMethod = acThread.getClass().getMethod("getApplication");
            application = (Application) appMethod.invoke(acThread);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    ExecutorService getExecutorService() {
        return executorService;
    }

    PosterDispatcher getPosterDispatcher() {
        return posterDispatcher;
    }

    DeviceCreator getDeviceCreator() {
        return deviceCreator;
    }

    Observable getObservable() {        
        return observable;
    }

    Logger getLogger() {
        return logger;
    }

    public ScannerType getScannerType() {
        return scanner == null ? null : scanner.getType();
    }

    public boolean isInitialized() {
        return isInitialized && application != null && instance != null;
    }

    /**
     * 蓝牙是否开启
     */
    public boolean isBluetoothOn() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    private class InnerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) { //蓝牙开关状态变化 
                if (bluetoothAdapter != null) {
                    //通知观察者蓝牙状态
                    observable.notifyObservers(MethodInfoGenerator.onBluetoothAdapterStateChanged(bluetoothAdapter.getState()));
                    if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) { //蓝牙关闭
                        logger.log(Log.DEBUG, Logger.TYPE_GENERAL, "蓝牙关闭了");
                        //通知搜索器
                        if (scanner != null) {
                            scanner.onBluetoothOff();
                        }
                        //断开所有连接
                        disconnectAllConnections();
                    } else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        logger.log(Log.DEBUG, Logger.TYPE_GENERAL, "蓝牙开启了");
                        //重连所有设置了自动重连的连接
                        for (Connection connection : connectionMap.values()) {
                            if (connection.isAutoReconnectEnabled()) {
                                connection.reconnect();
                            }
                        }
                    }
                }
            }
        }
    }

    public synchronized void initialize(@NonNull Application application) {
        if (isInitialized()) {
            return;
        }
        Inspector.requireNonNull(application, "application can't be");
        this.application = application;
        //检查是否支持BLE
        if (!application.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return;
        }
        //获取蓝牙配置器
        BluetoothManager bluetoothManager = (BluetoothManager) application.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null || bluetoothManager.getAdapter() == null) {
            return;
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        //注册蓝牙开关状态广播接收者
        if (broadcastReceiver == null) {
            broadcastReceiver = new InnerBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            application.registerReceiver(broadcastReceiver, filter);
        }        
        isInitialized = true;
    }

    private synchronized boolean checkStatus() {
        Inspector.requireNonNull(instance, "EasyBLE instance has been destroyed!");
        if (!isInitialized) {
            if (!tryAutoInit()) {
                String msg = "The SDK has not been initialized, make sure to call EasyBLE.getInstance().initialize(Application) first.";
                logger.log(Log.ERROR, Logger.TYPE_GENERAL, msg);
                return false;
            }
        } else if (application == null) {
            return tryAutoInit();
        }
        return true;
    }

    private boolean tryAutoInit() {
        tryGetApplication();
        if (application != null) {
            initialize(application);
        }
        return isInitialized();
    }

    /**
     * 日志输出控制
     */
    public void setLogEnabled(boolean isEnabled) {
        logger.setEnabled(isEnabled);
    }

    /**
     * 关闭所有连接并释放资源
     */
    public synchronized void release() {
        if (broadcastReceiver != null) {
            application.unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
        isInitialized = false;
        if (scanner != null) {
            scanner.release();
        }
        releaseAllConnections();
        if (internalObservable) {
            observable.unregisterAll();
            posterDispatcher.clearTasks();
        }
    }

    /**
     * 销毁，可重新构建
     */
    public void destroy() {
        release();
        synchronized (EasyBLE.class) {
            instance = null;
        }
    }

    /**
     * 注册连接状态及数据接收观察者
     */
    public void registerObserver(@NonNull EventObserver observer) {
        if (checkStatus()) {
            observable.registerObserver(observer);
        }
    }

    /**
     * 查询观察者是否注册
     */
    public boolean isObserverRegistered(@NonNull EventObserver observer) {
        return observable.isRegistered(observer);
    }

    /**
     * 取消注册连接状态及数据接收观察者
     */
    public void unregisterObserver(@NonNull EventObserver observer) {
        observable.unregisterObserver(observer);
    }

    /**
     * 通知所有观察者事件变化，通常只用在
     *
     * @param info 方法信息实例
     */
    public void notifyObservers(@NonNull MethodInfo info) {
        if (checkStatus()) {
            observable.notifyObservers(info);
        }
    }
    
    //检查并实例化搜索器
    private void checkAndInstanceScanner() {
        if (scanner == null) {
            synchronized (this) {
                if (bluetoothAdapter != null && scanner == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (scannerType == ScannerType.LEGACY) {
                            scanner = new LegacyScanner(this, bluetoothAdapter);
                        } else {
                            scanner = new LeScanner(this, bluetoothAdapter);
                        }
                    } else {
                        scanner = new LegacyScanner(this, bluetoothAdapter);
                    }
                }
            }
        }        
    }
    
    /**
     * 添加搜索监听器
     */
    public void addScanListener(@NonNull ScanListener listener) {
        checkAndInstanceScanner();
        if (checkStatus() && scanner != null) {
            scanner.addScanListener(listener);
        }
    }

    /**
     * 移除搜索监听器
     */
    public void removeScanListener(@NonNull ScanListener listener) {
        if (scanner != null) {
            scanner.removeScanListener(listener);
        }
    }

    /**
     * 是否正在搜索
     */
    public boolean isScanning() {
        return scanner != null && scanner.isScanning();
    }

    /**
     * 搜索BLE设备
     */
    public void startScan() {
        checkAndInstanceScanner();
        if (checkStatus() && scanner != null) {
            for (Connection connection : connectionMap.values()) {
                //连接过程中不允许搜索
                if (connection.getConnectionState() == ConnectionState.CONNECTING ||
                        connection.getConnectionState() == ConnectionState.SERVICE_DISCOVERING) {
                    return;
                }
            }
            scanner.startScan(application);
        }
    }

    /**
     * 停止搜索
     */
    public void stopScan() {
        if (checkStatus() && scanner != null) {
            scanner.stopScan(false);
        }
    }

    /**
     * 停止搜索，不触发回调
     */
    public void stopScanQuietly() {
        if (checkStatus() && scanner != null) {
            scanner.stopScan(true);
        }
    }

    /**
     * 创建连接
     *
     * @param address 蓝牙地址
     * @return 返回创建的连接实例，创建失败则返回null
     */
    @Nullable
    public Connection connect(@NonNull String address) {
        return connect(address, null, null);
    }

    /**
     * 创建连接
     *
     * @param address       蓝牙地址
     * @param configuration 连接配置
     * @return 返回创建的连接实例，创建失败则返回null
     */
    @Nullable
    public Connection connect(@NonNull String address, @NonNull ConnectionConfiguration configuration) {
        return connect(address, configuration, null);
    }

    /**
     * 创建连接
     *
     * @param address  蓝牙地址
     * @param observer 伴生观察者
     * @return 返回创建的连接实例，创建失败则返回null
     */
    @Nullable
    public Connection connect(@NonNull String address, @NonNull EventObserver observer) {
        return connect(address, null, observer);
    }

    /**
     * 创建连接
     *
     * @param address       蓝牙地址
     * @param configuration 连接配置
     * @param observer      伴生观察者
     * @return 返回创建的连接实例，创建失败则返回null
     */
    @Nullable
    public Connection connect(@NonNull String address, @Nullable ConnectionConfiguration configuration,
                              @Nullable EventObserver observer) {
        if (checkStatus()) {
            Inspector.requireNonNull(address, "address can't be null");
            BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(address);
            if (remoteDevice != null) {
                return connect(new Device(remoteDevice), configuration, observer);
            }
        }
        return null;
    }

    /**
     * 创建连接
     *
     * @param device 蓝牙设备实例
     * @return 返回创建的连接实例，创建失败则返回null
     */
    @Nullable
    public Connection connect(@NonNull Device device) {
        return connect(device, null, null);
    }

    /**
     * 创建连接
     *
     * @param device        蓝牙设备实例
     * @param configuration 连接配置
     * @return 返回创建的连接实例，创建失败则返回null
     */
    @Nullable
    public Connection connect(@NonNull Device device, @NonNull ConnectionConfiguration configuration) {
        return connect(device, configuration, null);
    }

    /**
     * 创建连接
     *
     * @param device   蓝牙设备实例
     * @param observer 伴生观察者
     * @return 返回创建的连接实例，创建失败则返回null
     */
    @Nullable
    public Connection connect(@NonNull Device device, @NonNull EventObserver observer) {
        return connect(device, null, observer);
    }

    /**
     * 创建连接
     *
     * @param device        蓝牙设备实例
     * @param configuration 连接配置
     * @param observer      伴生观察者
     * @return 返回创建的连接实例，创建失败则返回null
     */
    @Nullable
    public synchronized Connection connect(@NonNull final Device device, @Nullable ConnectionConfiguration configuration,
                                           @Nullable final EventObserver observer) {
        if (checkStatus()) {
            Inspector.requireNonNull(device, "device can't be null");
            Connection connection = connectionMap.remove(device.getAddress());
            //如果连接已存在，先释放掉
            if (connection != null) {
                connection.releaseNoEvent();
            }
            Boolean isConnectable = device.isConnectable();
            if (isConnectable == null || isConnectable) {
                int connectDelay = 0;
                if (bondController != null && bondController.accept(device)) {
                    BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());
                    if (remoteDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                        connectDelay = createBond(device.getAddress()) ? 1500 : 0;
                    }
                }
                connection = new ConnectionImpl(this, bluetoothAdapter, device, configuration, connectDelay, observer);
                connectionMap.put(device.address, connection);
                addressList.add(device.address);
                return connection;
            } else {
                String message = String.format(Locale.US, "connect failed! [type: unconnectable, name: %s, addr: %s]",
                        device.getName(), device.getAddress());
                logger.log(Log.ERROR, Logger.TYPE_CONNECTION_STATE, message);
                if (observer != null) {
                    posterDispatcher.post(observer, MethodInfoGenerator.onConnectFailed(device, Connection.CONNECT_FAIL_TYPE_CONNECTION_IS_UNSUPPORTED));
                }
                observable.notifyObservers(MethodInfoGenerator.onConnectFailed(device, Connection.CONNECT_FAIL_TYPE_CONNECTION_IS_UNSUPPORTED));
            }
        }
        return null;
    }

    /**
     * 获取所有连接，无序的
     */
    @NonNull
    public Collection<Connection> getConnections() {
        return connectionMap.values();
    }

    /**
     * 获取所有连接，有序的
     */
    @NonNull
    public List<Connection> getOrderedConnections() {
        List<Connection> list = new ArrayList<>();
        for (String address : addressList) {
            Connection connection = connectionMap.get(address);
            if (connection != null) {
                list.add(connection);
            }
        }
        return list;
    }

    /**
     * 获取第一个连接
     */
    @Nullable
    public Connection getFirstConnection() {
        return addressList.isEmpty() ? null : connectionMap.get(addressList.get(0));
    }

    /**
     * 获取最后一个连接
     */
    @Nullable
    public Connection getLastConnection() {
        return addressList.isEmpty() ? null : connectionMap.get(addressList.get(addressList.size() - 1));
    }

    @Nullable
    public Connection getConnection(Device device) {
        return device == null ? null : connectionMap.get(device.getAddress());
    }

    @Nullable
    public Connection getConnection(String address) {
        return address == null ? null : connectionMap.get(address);
    }

    /**
     * 断开连接
     */
    public void disconnectConnection(Device device) {
        if (checkStatus() && device != null) {
            Connection connection = connectionMap.get(device.getAddress());
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 断开连接
     */
    public void disconnectConnection(String address) {
        if (checkStatus() && address != null) {
            Connection connection = connectionMap.get(address);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 断开所有连接
     */
    public void disconnectAllConnections() {
        if (checkStatus()) {
            for (Connection connection : connectionMap.values()) {
                connection.disconnect();
            }
        }
    }

    /**
     * 释放所有连接
     */
    public void releaseAllConnections() {
        if (checkStatus()) {
            for (Connection connection : connectionMap.values()) {
                connection.release();
            }
            connectionMap.clear();
            addressList.clear();
        }
    }

    /**
     * 释放连接
     */
    public void releaseConnection(String address) {
        if (checkStatus() && address != null) {
            addressList.remove(address);
            Connection connection = connectionMap.remove(address);
            if (connection != null) {
                connection.release();
            }
        }
    }

    /**
     * 释放连接
     */
    public void releaseConnection(Device device) {
        if (checkStatus() && device != null) {
            addressList.remove(device.getAddress());
            Connection connection = connectionMap.remove(device.getAddress());
            if (connection != null) {
                connection.release();
            }
        }
    }

    /**
     * 重连所有设备
     */
    public void reconnectAll() {
        if (checkStatus()) {
            for (Connection connection : connectionMap.values()) {
                if (connection.getConnectionState() != ConnectionState.SERVICE_DISCOVERED) {
                    connection.reconnect();
                }
            }
        }
    }

    /**
     * 重连设备
     */
    public void reconnect(Device device) {
        if (checkStatus() && device != null) {
            Connection connection = connectionMap.get(device.getAddress());
            if (connection != null && connection.getConnectionState() != ConnectionState.SERVICE_DISCOVERED) {
                connection.reconnect();
            }
        }
    }

    /**
     * 根据MAC地址获取设备的配对状态
     *
     * @return {@link BluetoothDevice#BOND_NONE}，{@link BluetoothDevice#BOND_BONDED}，{@link BluetoothDevice#BOND_BONDING}
     */
    public int getBondState(@NonNull String address) {
        checkStatus();
        try {
            return bluetoothAdapter.getRemoteDevice(address).getBondState();
        } catch (Exception e) {
            return BluetoothDevice.BOND_NONE;
        }
    }

    /**
     * 开始配对
     *
     * @param address 设备地址
     */
    public boolean createBond(@NonNull String address) {
        checkStatus();
        try {
            BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(address);
            return remoteDevice.getBondState() != BluetoothDevice.BOND_NONE || remoteDevice.createBond();
        } catch (Exception ignore) {
            return false;
        }
    }

    /**
     * 根据过滤器，清除配对
     */
    @SuppressWarnings("all")
    public void clearBondDevices(RemoveBondFilter filter) {
        checkStatus();
        if (bluetoothAdapter != null) {
            Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : devices) {
                if (filter == null || filter.accept(device)) {
                    try {
                        device.getClass().getMethod("removeBond").invoke(device);
                    } catch (Exception ignore) {
                    }
                }
            }
        }
    }

    /**
     * 解除配对
     *
     * @param address 设备地址
     */
    @SuppressWarnings("all")
    public void removeBond(@NonNull String address) {
        checkStatus();
        try {
            BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(address);
            if (remoteDevice.getBondState() != BluetoothDevice.BOND_NONE) {
                remoteDevice.getClass().getMethod("removeBond").invoke(remoteDevice);
            }
        } catch (Exception ignore) {
        }
    }
}

package cn.wandersnail.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import cn.wandersnail.ble.callback.RequestCallback;
import cn.wandersnail.ble.callback.ScanListener;
import cn.wandersnail.ble.util.Logger;
import cn.wandersnail.commons.observer.Observable;
import cn.wandersnail.commons.poster.MethodInfo;
import cn.wandersnail.commons.poster.PosterDispatcher;
import cn.wandersnail.commons.util.MathUtils;
import cn.wandersnail.commons.util.StringUtils;

/**
 * date: 2019/8/3 19:47
 * author: zengfansheng
 */
class ConnectionImpl implements Connection, ScanListener {
    private static final int MSG_REQUEST_TIMEOUT = 0;
    private static final int MSG_CONNECT = 1;
    private static final int MSG_DISCONNECT = 2;
    private static final int MSG_REFRESH = 3;
    private static final int MSG_TIMER = 4;
    private static final int MSG_DISCOVER_SERVICES = 6;
    private static final int MSG_ON_CONNECTION_STATE_CHANGE = 7;
    private static final int MSG_ON_SERVICES_DISCOVERED = 8;

    private static final int MSG_ARG_NONE = 0;
    private static final int MSG_ARG_RECONNECT = 1;

    private final BluetoothAdapter bluetoothAdapter;
    private final Device device;
    private final ConnectionConfiguration configuration;//连接配置
    private BluetoothGatt bluetoothGatt;
    private final List<GenericRequest> requestQueue = new ArrayList<>();//请求队列
    private GenericRequest currentRequest;//当前的请求
    private final EventObserver observer;//伴生观察者
    private boolean isReleased;//连接是否已释放
    private final Handler connHandler;//用于操作连接的Handler，运行在主线程
    private long connStartTime; //用于连接超时计时
    private int refreshCount;//刷新（清缓存）计数，在发现服务后清零
    private int tryReconnectCount;//尝试重连计数
    private ConnectionState lastConnectionState;//上次连接状态
    private int reconnectImmediatelyCount = 0; //不搜索直接重连计数
    private boolean refreshing;//是否正在执行清理缓存
    private boolean isActiveDisconnect;//是否主动断开连接
    private long lastScanStopTime;//上次搜索停止时间
    private final Logger logger;
    private final Observable observable;
    private final PosterDispatcher posterDispatcher;
    private final BluetoothGattCallback gattCallback = new BleGattCallback();
    private final EasyBLE easyBle;
    private int mtu = 23;
    private BluetoothGattCallback originCallback;
    private boolean connectFailed;//连接失败过

    ConnectionImpl(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter, Device device, ConnectionConfiguration configuration,
                   int connectDelay, EventObserver observer) {
        this.easyBle = easyBle;
        this.bluetoothAdapter = bluetoothAdapter;
        this.device = device;
        //如果没有配置
        if (configuration == null) {
            this.configuration = new ConnectionConfiguration();
        } else {
            this.configuration = configuration;
        }
        this.observer = observer;
        logger = easyBle.getLogger();
        observable = easyBle.getObservable();
        posterDispatcher = easyBle.getPosterDispatcher();
        connHandler = new ConnHandler(this);
        connStartTime = System.currentTimeMillis();
        connHandler.sendEmptyMessageDelayed(MSG_CONNECT, connectDelay); //执行连接
        connHandler.sendEmptyMessageDelayed(MSG_TIMER, connectDelay); //启动定时器
        easyBle.addScanListener(this);
    }

    @Override
    public void onScanStart() {
    }

    @Override
    public void onScanStop() {
        synchronized (this) {
            lastScanStopTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onScanResult(@NonNull Device device, boolean isConnectedBySys) {
        synchronized (this) {
            if (!isReleased && this.device.equals(device) && this.device.connectionState == ConnectionState.SCANNING_FOR_RECONNECTION) {
                connHandler.sendEmptyMessage(MSG_CONNECT);
            }
        }
    }

    @Override
    public void onScanError(int errorCode, @NonNull String errorMsg) {

    }

    @Override
    public void setBluetoothGattCallback(BluetoothGattCallback callback) {
        originCallback = callback;
    }

    @Override
    public boolean hasProperty(UUID service, UUID characteristic, int property) {
        BluetoothGattCharacteristic charac = getCharacteristic(service, characteristic);
        if (charac == null) {
            return false;
        }
        return (charac.getProperties() & property) != 0;
    }

    private class BleGattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (originCallback != null) {
                easyBle.getExecutorService().execute(() -> originCallback.onConnectionStateChange(gatt, status, newState));
            }
            if (!isReleased) {
                Message.obtain(connHandler, MSG_ON_CONNECTION_STATE_CHANGE, status, newState).sendToTarget();
            } else {
                closeGatt(gatt);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (originCallback != null) {
                easyBle.getExecutorService().execute(() -> originCallback.onServicesDiscovered(gatt, status));
            }
            if (!isReleased) {
                Message.obtain(connHandler, MSG_ON_SERVICES_DISCOVERED, status, 0).sendToTarget();
            } else {
                closeGatt(gatt);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (originCallback != null) {
                easyBle.getExecutorService().execute(() -> originCallback.onCharacteristicRead(gatt, characteristic, status));
            }
            if (currentRequest != null) {
                if (currentRequest.type == RequestType.READ_CHARACTERISTIC) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        notifyCharacteristicRead(currentRequest, characteristic.getValue());
                    } else {
                        handleGattStatusFailed(status);
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (originCallback != null) {
                easyBle.getExecutorService().execute(() -> originCallback.onCharacteristicWrite(gatt, characteristic, status));
            }
            if (currentRequest != null && currentRequest.type == RequestType.WRITE_CHARACTERISTIC &&
                    currentRequest.writeOptions.isWaitWriteResult) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (logger.isEnabled()) {
                        byte[] data = (byte[]) currentRequest.value;//完整包数据
                        int packageSize = currentRequest.writeOptions.packageSize;
                        int total = data.length / packageSize + (data.length % packageSize == 0 ? 0 : 1);
                        int progress;
                        if (currentRequest.remainQueue == null || currentRequest.remainQueue.isEmpty()) {
                            progress = total;
                        } else {
                            progress = data.length / packageSize - currentRequest.remainQueue.size() + 1;
                        }
                        printWriteLog(currentRequest, progress, total, characteristic.getValue());
                    }
                    if (currentRequest.remainQueue == null || currentRequest.remainQueue.isEmpty()) {
                        notifyCharacteristicWrite(currentRequest, (byte[]) currentRequest.value);
                        executeNextRequest();
                    } else {
                        connHandler.removeMessages(MSG_REQUEST_TIMEOUT);
                        connHandler.sendMessageDelayed(Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, currentRequest),
                                configuration.requestTimeoutMillis);
                        GenericRequest req = currentRequest;
                        int delay = currentRequest.writeOptions.packageWriteDelayMillis;
                        if (delay > 0) {
                            try {
                                Thread.sleep(delay);
                            } catch (InterruptedException ignore) {
                            }
                            if (req != currentRequest) {
                                return;
                            }
                        }
                        req.sendingBytes = req.remainQueue.remove();
                        write(req, characteristic, req.sendingBytes);
                    }
                } else {
                    handleFailedCallback(currentRequest, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, status, true);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (originCallback != null) {
                easyBle.getExecutorService().execute(() -> originCallback.onCharacteristicChanged(gatt, characteristic));
            }
            notifyCharacteristicChanged(characteristic);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            device.setRssi(rssi);
            if (originCallback != null) {
                easyBle.getExecutorService().execute(() -> originCallback.onReadRemoteRssi(gatt, rssi, status));
            }
            if (currentRequest != null) {
                if (currentRequest.type == RequestType.READ_RSSI) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        notifyRssiRead(currentRequest, rssi);
                    } else {
                        handleGattStatusFailed(status);
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (originCallback != null) {
                easyBle.getExecutorService().execute(() -> originCallback.onDescriptorRead(gatt, descriptor, status));
            }
            if (currentRequest != null) {
                if (currentRequest.type == RequestType.READ_DESCRIPTOR) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        notifyDescriptorRead(currentRequest, descriptor.getValue());
                    } else {
                        handleGattStatusFailed(status);
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (originCallback != null) {
                easyBle.getExecutorService().execute(() -> originCallback.onDescriptorWrite(gatt, descriptor, status));
            }
            if (currentRequest != null) {
                if (currentRequest.type == RequestType.SET_NOTIFICATION || currentRequest.type == RequestType.SET_INDICATION) {
                    BluetoothGattDescriptor localDescriptor = getDescriptor(descriptor.getCharacteristic().getService().getUuid(),
                            descriptor.getCharacteristic().getUuid(), clientCharacteristicConfig);
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        handleGattStatusFailed(status);
                        if (localDescriptor != null) {
                            localDescriptor.setValue(currentRequest.descriptorTemp);
                        }
                    } else {
                        notifyNotificationOrIndicationChanged(currentRequest, ((int) currentRequest.value) == 1);
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (originCallback != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    easyBle.getExecutorService().execute(() -> originCallback.onMtuChanged(gatt, mtu, status));
                }
            }
            if (currentRequest != null) {
                if (currentRequest.type == RequestType.CHANGE_MTU) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        ConnectionImpl.this.mtu = mtu;
                        notifyMtuChanged(currentRequest, mtu);
                    } else {
                        handleGattStatusFailed(status);
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            if (originCallback != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    easyBle.getExecutorService().execute(() -> originCallback.onPhyRead(gatt, txPhy, rxPhy, status));
                }
            }
            handlePhyChange(true, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            if (originCallback != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    easyBle.getExecutorService().execute(() -> originCallback.onPhyRead(gatt, txPhy, rxPhy, status));
                }
            }
            handlePhyChange(false, txPhy, rxPhy, status);
        }
    }

    private void doOnConnectionStateChange(int status, int newState) {
        if (bluetoothGatt != null) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    logD(Logger.TYPE_CONNECTION_STATE, "connected! [name: %s, addr: %s]", device.name, device.address);
                    device.connectionState = ConnectionState.CONNECTED;
                    sendConnectionCallback();
                    // 延时一会再去发现服务
                    connHandler.sendEmptyMessageDelayed(MSG_DISCOVER_SERVICES, configuration.discoverServicesDelayMillis);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    logD(Logger.TYPE_CONNECTION_STATE, "disconnected! [name: %s, addr: %s, autoReconnEnable: %s]",
                            device.name, device.address, configuration.isAutoReconnect);
                    clearRequestQueueAndNotify();
                    notifyDisconnected();
                }
            } else {
                logE(Logger.TYPE_CONNECTION_STATE, "GATT error! [status: %d, name: %s, addr: %s]",
                        status, device.name, device.address);
                connectFailed = true;
                notifyConnectionError(device, status);
                if (status == 133) {
                    doClearTaskAndRefresh();
                } else {
                    clearRequestQueueAndNotify();
                    notifyDisconnected();
                }
            }
        }
    }

    private void doOnServicesDiscovered(int status) {
        if (bluetoothGatt != null) {
            List<BluetoothGattService> services = bluetoothGatt.getServices();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logD(Logger.TYPE_CONNECTION_STATE, "services discovered! [name: %s, addr: %s, size: %d]", device.name,
                        device.address, services.size());
                if (services.isEmpty()) {
                    doClearTaskAndRefresh();
                } else {
                    refreshCount = 0;
                    tryReconnectCount = 0;
                    reconnectImmediatelyCount = 0;
                    device.connectionState = ConnectionState.SERVICE_DISCOVERED;
                    sendConnectionCallback();
                }
            } else {
                connectFailed = true;
                notifyConnectionError(device, status);
                doClearTaskAndRefresh();
                logE(Logger.TYPE_CONNECTION_STATE, "GATT error! [status: %d, name: %s, addr: %s]",
                        status, device.name, device.address);
            }
        }
    }

    private void doDiscoverServices() {
        if (bluetoothGatt != null) {
            bluetoothGatt.discoverServices();
            device.connectionState = ConnectionState.SERVICE_DISCOVERING;
            sendConnectionCallback();
        } else {
            notifyDisconnected();
        }
    }

    private void doTimer() {
        if (!isReleased) {
            //只处理不是已发现服务并且不在刷新也不是主动断开连接的
            if (device.connectionState != ConnectionState.SERVICE_DISCOVERED && !refreshing && !isActiveDisconnect) {
                if (device.connectionState != ConnectionState.DISCONNECTED) {
                    //超时
                    if (System.currentTimeMillis() - connStartTime > configuration.connectTimeoutMillis) {
                        connStartTime = System.currentTimeMillis();
                        logE(Logger.TYPE_CONNECTION_STATE, "connect timeout! [name: %s, addr: %s]", device.name, device.address);
                        int type;
                        switch (device.connectionState) {
                            case SCANNING_FOR_RECONNECTION:
                                type = TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE;
                                break;
                            case CONNECTING:
                                type = TIMEOUT_TYPE_CANNOT_CONNECT;
                                break;
                            default:
                                type = TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES;
                                break;
                        }
                        observable.notifyObservers(MethodInfoGenerator.onConnectTimeout(device, type));
                        if (observer != null) {
                            posterDispatcher.post(observer, MethodInfoGenerator.onConnectTimeout(device, type));
                        }
                        boolean infinite = configuration.tryReconnectMaxTimes == ConnectionConfiguration.TRY_RECONNECT_TIMES_INFINITE;
                        if (configuration.isAutoReconnect && (infinite || tryReconnectCount < configuration.tryReconnectMaxTimes)) {
                            doDisconnect(true);
                        } else {
                            doDisconnect(false);
                            if (observer != null) {
                                posterDispatcher.post(observer, MethodInfoGenerator.onConnectFailed(device, CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION));
                            }
                            observable.notifyObservers(MethodInfoGenerator.onConnectFailed(device, CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION));
                            logE(Logger.TYPE_CONNECTION_STATE, "connect failed! [type: maximun reconnection, name: %s, addr: %s]",
                                    device.name, device.address);
                        }
                    }
                } else if (configuration.isAutoReconnect) {
                    doDisconnect(true);
                }
            }
            connHandler.sendEmptyMessageDelayed(MSG_TIMER, 500);
        }
    }

    private final Runnable connectRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isReleased) {
                //连接之前必须先停止搜索
                easyBle.stopScan();
                boolean autoConnect = configuration.useAutoConnect;
                if (!autoConnect && connectFailed && configuration.useAutoConnectAfterConnectionFailure) {
                    autoConnect = true;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    bluetoothGatt = device.getOriginDevice().connectGatt(easyBle.getContext(), autoConnect, gattCallback,
                            configuration.transport, configuration.phy);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bluetoothGatt = device.getOriginDevice().connectGatt(easyBle.getContext(), autoConnect, gattCallback,
                            configuration.transport);
                } else {
                    bluetoothGatt = device.getOriginDevice().connectGatt(easyBle.getContext(), autoConnect, gattCallback);
                }
            }
        }
    };

    private void doConnect() {
        cancelRefreshState();
        device.connectionState = ConnectionState.CONNECTING;
        sendConnectionCallback();
        logD(Logger.TYPE_CONNECTION_STATE, "connecting [name: %s, addr: %s]", device.name, device.address);
        connHandler.postDelayed(connectRunnable, 500);
    }

    /**
     * 处理断开
     *
     * @param reconnect 断开后是否重连
     */
    private void doDisconnect(boolean reconnect) {
        clearRequestQueueAndNotify();
        connHandler.removeCallbacks(connectRunnable);
        connHandler.removeMessages(MSG_DISCOVER_SERVICES);
        if (bluetoothGatt != null) {
            closeGatt(bluetoothGatt);
            bluetoothGatt = null;
        }
        device.connectionState = ConnectionState.DISCONNECTED;
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled() && reconnect && !isReleased) {
            if (reconnectImmediatelyCount < configuration.reconnectImmediatelyMaxTimes) {
                tryReconnectCount++;
                reconnectImmediatelyCount++;
                connStartTime = System.currentTimeMillis();
                doConnect();
                return;
            } else if (canScanReconnect()) {
                tryScanReconnect();
            }
        }
        sendConnectionCallback();
    }

    private void doClearTaskAndRefresh() {
        clearRequestQueueAndNotify();
        doRefresh(true);
    }

    //处理刷新
    private void doRefresh(boolean isAuto) {
        logD(Logger.TYPE_CONNECTION_STATE, "refresh GATT! [name: %s, addr: %s]", device.name, device.address);
        connStartTime = System.currentTimeMillis();
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
            } catch (Throwable ignore) {
            }

            if (isAuto) {
                if (refreshCount <= 5) {
                    refreshing = doRefresh();
                }
                refreshCount++;
            } else {
                refreshing = doRefresh();
            }
            if (refreshing) {
                connHandler.postDelayed(this::cancelRefreshState, 2000);
            } else if (bluetoothGatt != null) {
                closeGatt(bluetoothGatt);
                bluetoothGatt = null;
            }
        }
        notifyDisconnected();
    }

    private void cancelRefreshState() {
        if (refreshing) {
            refreshing = false;
            if (bluetoothGatt != null) {
                closeGatt(bluetoothGatt);
                bluetoothGatt = null;
            }
        }
    }

    private void tryScanReconnect() {
        if (!isReleased) {
            tryReconnectCount++;
            connStartTime = System.currentTimeMillis();
            easyBle.stopScan();
            //搜索设备，搜索到才执行连接
            device.connectionState = ConnectionState.SCANNING_FOR_RECONNECTION;
            logD(Logger.TYPE_CONNECTION_STATE, "scanning for reconnection [name: %s, addr: %s]", device.name, device.address);
            easyBle.startScan();
        }
    }

    private boolean canScanReconnect() {
        long duration = System.currentTimeMillis() - lastScanStopTime;
        List<Pair<Integer, Integer>> parameters = configuration.scanIntervalPairsInAutoReconnection;
        Collections.sort(parameters, (o1, o2) -> {
            if (o1 == null || o1.first == null) return 1;
            if (o2 == null || o2.first == null) return -1;
            return o2.first.compareTo(o1.first);
        });
        for (Pair<Integer, Integer> pair : parameters) {
            if (pair.first != null && pair.second != null && tryReconnectCount >= pair.first && duration >= pair.second) {
                return true;
            }
        }
        return false;
    }

    private void closeGatt(BluetoothGatt gatt) {
        try {
            gatt.disconnect();
        } catch (Throwable ignore) {
        }
        try {
            gatt.close();
        } catch (Throwable ignore) {
        }
    }

    private void notifyDisconnected() {
        device.connectionState = ConnectionState.DISCONNECTED;
        sendConnectionCallback();
    }

    private void sendConnectionCallback() {
        if (lastConnectionState != device.connectionState) {
            lastConnectionState = device.connectionState;
            if (observer != null) {
                posterDispatcher.post(observer, MethodInfoGenerator.onConnectionStateChanged(device));
            }
            observable.notifyObservers(MethodInfoGenerator.onConnectionStateChanged(device));
        }
    }

    private boolean write(GenericRequest request, BluetoothGattCharacteristic characteristic, byte[] value) {
        characteristic.setValue(value);
        int writeType = request.writeOptions.writeType;
        if ((writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE ||
                writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED ||
                writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)) {
            characteristic.setWriteType(writeType);
        }
        if (bluetoothGatt == null) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_GATT_IS_NULL, true);
            return false;
        }
        if (!bluetoothGatt.writeCharacteristic(characteristic)) {
            handleWriteFailed(request);
            return false;
        }
        return true;
    }

    private void handleWriteFailed(GenericRequest request) {
        connHandler.removeMessages(MSG_REQUEST_TIMEOUT);
        request.remainQueue = null;
        handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
    }

    private boolean enableNotificationOrIndicationFail(boolean enable, boolean notification, BluetoothGattCharacteristic characteristic) {
        if (!bluetoothAdapter.isEnabled() || bluetoothGatt == null || !bluetoothGatt
                .setCharacteristicNotification(characteristic, enable)) {
            return true;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(clientCharacteristicConfig);
        if (descriptor == null) {
            return true;
        }
        byte[] originValue = descriptor.getValue();
        if (currentRequest != null) {
            if (currentRequest.type == RequestType.SET_NOTIFICATION || currentRequest.type == RequestType.SET_INDICATION) {
                currentRequest.descriptorTemp = originValue;
            }
        }
        if (enable) {
            if (notification) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            }
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        // There was a bug in Android up to 6.0 where the descriptor was written using parent
        // characteristic's write type, instead of always Write With Response, as the spec says.
        int writeType = characteristic.getWriteType();
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        boolean result = bluetoothGatt.writeDescriptor(descriptor);
        if (!enable) {
            //还原原始值
            descriptor.setValue(originValue);
        }
        characteristic.setWriteType(writeType);
        return !result;
    }

    private static class ConnHandler extends Handler {
        private final WeakReference<ConnectionImpl> weakRef;

        ConnHandler(ConnectionImpl connection) {
            super(Looper.getMainLooper());
            weakRef = new WeakReference<>(connection);
        }

        @Override
        public void handleMessage(Message msg) {
            ConnectionImpl connection = weakRef.get();
            if (connection != null) {
                if (connection.isReleased) {
                    return;
                }
                switch (msg.what) {
                    case MSG_REQUEST_TIMEOUT:
                        GenericRequest request = (GenericRequest) msg.obj;
                        if (connection.currentRequest != null && connection.currentRequest == request) {
                            connection.handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_TIMEOUT, false);
                            connection.executeNextRequest();
                        }
                        break;
                    case MSG_CONNECT://连接   
                        if (connection.bluetoothAdapter.isEnabled()) {
                            connection.doConnect();
                        }
                        break;
                    case MSG_DISCONNECT://断开
                        boolean reconnect = msg.arg1 == MSG_ARG_RECONNECT && connection.bluetoothAdapter.isEnabled();
                        connection.doDisconnect(reconnect);
                        break;
                    case MSG_REFRESH://手动刷新
                        connection.doRefresh(false);
                        break;
                    case MSG_TIMER://定时器
                        connection.doTimer();
                        break;
                    case MSG_DISCOVER_SERVICES://执行发现服务
                    case MSG_ON_CONNECTION_STATE_CHANGE://连接状态变化
                    case MSG_ON_SERVICES_DISCOVERED://服务已发现
                        if (connection.bluetoothAdapter.isEnabled()) {
                            if (msg.what == MSG_DISCOVER_SERVICES) {
                                connection.doDiscoverServices();
                            } else if (msg.what == MSG_ON_SERVICES_DISCOVERED) {
                                connection.doOnServicesDiscovered(msg.arg1);
                            } else {
                                connection.doOnConnectionStateChange(msg.arg1, msg.arg2);
                            }
                        }
                        break;
                }
            }
        }
    }

    private void enqueue(GenericRequest request) {
        if (isReleased) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_CONNECTION_RELEASED, false);
        } else {
            synchronized (this) {
                if (currentRequest == null) {
                    executeRequest(request);
                } else {
                    //根据优化级将请求插入队列中
                    int index = -1;
                    for (int i = 0; i < requestQueue.size(); i++) {
                        GenericRequest req = requestQueue.get(i);
                        if (req.priority >= request.priority) {
                            if (i < requestQueue.size() - 1) {
                                if (requestQueue.get(i + 1).priority < request.priority) {
                                    index = i + 1;
                                    break;
                                }
                            } else {
                                index = i + 1;
                            }
                        }
                    }
                    if (index == -1) {
                        requestQueue.add(0, request);
                    } else if (index >= requestQueue.size()) {
                        requestQueue.add(request);
                    } else {
                        requestQueue.add(index, request);
                    }
                }
            }
        }
    }

    private void executeNextRequest() {
        synchronized (this) {
            connHandler.removeMessages(MSG_REQUEST_TIMEOUT);
            if (requestQueue.isEmpty()) {
                currentRequest = null;
            } else {
                executeRequest(requestQueue.remove(0));
            }
        }
    }

    private void executeRequest(GenericRequest request) {
        currentRequest = request;
        connHandler.sendMessageDelayed(Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, request), configuration.requestTimeoutMillis);
        if (bluetoothAdapter.isEnabled()) {
            if (bluetoothGatt != null) {
                switch (request.type) {
                    case READ_RSSI:
                        if (!bluetoothGatt.readRemoteRssi()) {
                            handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
                        }
                        break;
                    case CHANGE_MTU:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            if (!bluetoothGatt.requestMtu((int) request.value)) {
                                handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
                            }
                        }
                        break;
                    case READ_PHY:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            bluetoothGatt.readPhy();
                        }
                        break;
                    case SET_PREFERRED_PHY:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            int[] options = (int[]) request.value;
                            bluetoothGatt.setPreferredPhy(options[0], options[1], options[2]);
                        }
                        break;
                    default:
                        BluetoothGattService gattService = bluetoothGatt.getService(request.service);
                        if (gattService != null) {
                            BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(request.characteristic);
                            if (characteristic != null) {
                                switch (request.type) {
                                    case SET_NOTIFICATION:
                                    case SET_INDICATION:
                                        executeIndicationOrNotification(request, characteristic);
                                        break;
                                    case READ_CHARACTERISTIC:
                                        executeReadCharacteristic(request, characteristic);
                                        break;
                                    case READ_DESCRIPTOR:
                                        executeReadDescriptor(request, characteristic);
                                        break;
                                    case WRITE_CHARACTERISTIC:
                                        executeWriteCharacteristic(request, characteristic);
                                        break;
                                }
                            } else {
                                handleFailedCallback(request, REQUEST_FAIL_TYPE_CHARACTERISTIC_NOT_EXIST, true);
                            }
                        } else {
                            handleFailedCallback(request, REQUEST_FAIL_TYPE_SERVICE_NOT_EXIST, true);
                        }
                        break;
                }
            } else {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_GATT_IS_NULL, true);
            }
        } else {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_BLUETOOTH_ADAPTER_DISABLED, true);
        }
    }

    private void printWriteLog(GenericRequest request, int progress, int total, byte[] value) {
        if (logger.isEnabled()) {
            String t = String.valueOf(total);
            StringBuilder sb = new StringBuilder(String.valueOf(progress));
            while (sb.length() < t.length()) {
                sb.insert(0, "0");
            }
            logD(Logger.TYPE_CHARACTERISTIC_WRITE, "package [%s/%s] write success! [UUID: %s, addr: %s, value: %s]",
                    sb, t, substringUuid(request.characteristic), device.address, toHex(value));
        }
    }

    private void executeWriteCharacteristic(GenericRequest request, BluetoothGattCharacteristic characteristic) {
        try {
            byte[] value = (byte[]) request.value;
            WriteOptions options = request.writeOptions;
            int reqDelay = options.requestWriteDelayMillis > 0 ? options.requestWriteDelayMillis : options.packageWriteDelayMillis;
            if (reqDelay > 0) {
                try {
                    Thread.sleep(reqDelay);
                } catch (InterruptedException ignore) {
                }
                if (request != currentRequest) {
                    return;
                }
            }
            if (options.useMtuAsPackageSize) {
                options.packageSize = mtu - 3;
            }
            if (value.length > options.packageSize) {
                List<byte[]> list = MathUtils.splitPackage(value, options.packageSize);
                if (!options.isWaitWriteResult) { //不等待写入回调，直接写入下一包数据
                    int delay = options.packageWriteDelayMillis;
                    for (int i = 0; i < list.size(); i++) {
                        byte[] bytes = list.get(i);
                        if (i > 0 && delay > 0) {
                            try {
                                Thread.sleep(delay);
                            } catch (InterruptedException ignore) {
                            }
                            if (request != currentRequest) {
                                return;
                            }
                        }
                        if (!write(request, characteristic, bytes)) {
                            return;
                        } else {
                            printWriteLog(request, i + 1, list.size(), bytes);
                        }
                    }
                    printWriteLog(request, list.size(), list.size(), list.get(list.size() - 1));
                } else { //发送第一包，剩下的加入队列
                    request.remainQueue = new ConcurrentLinkedQueue<>();
                    request.remainQueue.addAll(list);
                    request.sendingBytes = request.remainQueue.remove();
                    write(request, characteristic, request.sendingBytes);
                }
            } else {
                request.sendingBytes = value;
                if (write(request, characteristic, value)) {
                    if (!options.isWaitWriteResult) {
                        notifyCharacteristicWrite(request, value);
                        printWriteLog(request, 1, 1, value);
                        executeNextRequest();
                    }
                }
            }
        } catch (Throwable e) {
            handleWriteFailed(request);
        }
    }

    private void executeReadDescriptor(GenericRequest request, BluetoothGattCharacteristic characteristic) {
        BluetoothGattDescriptor gattDescriptor = characteristic.getDescriptor(request.descriptor);
        if (gattDescriptor != null) {
            if (!bluetoothGatt.readDescriptor(gattDescriptor)) {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
            }
        } else {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_DESCRIPTOR_NOT_EXIST, true);
        }
    }

    private void executeReadCharacteristic(GenericRequest request, BluetoothGattCharacteristic characteristic) {
        if (!bluetoothGatt.readCharacteristic(characteristic)) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
        }
    }

    private void executeIndicationOrNotification(GenericRequest request, BluetoothGattCharacteristic characteristic) {
        if (enableNotificationOrIndicationFail(((int) request.value) == 1,
                request.type == RequestType.SET_NOTIFICATION, characteristic)) {
            handleGattStatusFailed(-1);
            executeNextRequest();
        }
    }
    
    private void handlePhyChange(boolean read, int txPhy, int rxPhy, int status) {
        if (currentRequest != null) {
            if ((read && currentRequest.type == RequestType.READ_PHY) || ((!read && currentRequest.type == RequestType.SET_PREFERRED_PHY))) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    notifyPhyChange(currentRequest, txPhy, rxPhy);
                } else {
                    handleGattStatusFailed(status);
                }
                executeNextRequest();
            }
        }
    }

    private void handleGattStatusFailed(int status) {
        if (currentRequest != null) {
            connHandler.removeMessages(MSG_REQUEST_TIMEOUT);//移除超时检测
            handleFailedCallback(currentRequest, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, status, false);
        }
    }

    private void handleFailedCallback(GenericRequest request, int failType, int status, boolean executeNext) {
        notifyRequestFailed(request, failType, status);
        if (executeNext) {
            executeNextRequest();
        }
    }
    
    private void handleFailedCallback(GenericRequest request, int failType, boolean executeNext) {
        handleFailedCallback(request, failType, -1, executeNext);
    }

    private String toHex(byte[] bytes) {
        return StringUtils.toHex(bytes);
    }

    private String substringUuid(UUID uuid) {
        return uuid == null ? "null" : uuid.toString().substring(0, 8);
    }

    private void handleCallbacks(RequestCallback callback, MethodInfo info) {
        if (observer != null) {
            posterDispatcher.post(observer, info);//通知伴生观察者
        }
        if (callback != null) {//回调方式
            posterDispatcher.post(callback, info);
        } else {//观察者模式
            observable.notifyObservers(info);
        }
    }

    private void log(int priority, int type, String format, Object... args) {
        logger.log(priority, type, String.format(Locale.US, format, args));
    }

    private void logE(int type, String format, Object... args) {
        log(Log.ERROR, type, format, args);
    }

    private void logD(int type, String format, Object... args) {
        log(Log.DEBUG, type, format, args);
    }

    private void notifyConnectionError(Device device, int status) {
        MethodInfo info = MethodInfoGenerator.onConnectionError(status);
        MethodInfo info1 = MethodInfoGenerator.onConnectionError(device, status);
        observable.notifyObservers(info1);
        observable.notifyObservers(info);
        if (observer != null) {
            posterDispatcher.post(observer, info1);
            posterDispatcher.post(observer, info);
        }
    }
    
    private void notifyRequestFailed(GenericRequest request, int failType, int status) {
        MethodInfo info = MethodInfoGenerator.onRequestFailed(request, failType, request.value);
        MethodInfo info1 = MethodInfoGenerator.onRequestFailed(request, failType, status, request.value);
        handleCallbacks(request.callback, info);
        handleCallbacks(request.callback, info1);
        if (status != -1) {
            logE(Logger.TYPE_REQUEST_FAILED, "request failed! [requestType: %s, addr: %s, failType: %d，gatt status：%d",
                    request.type, device.address, failType, status);
        } else {
            logE(Logger.TYPE_REQUEST_FAILED, "request failed! [requestType: %s, addr: %s, failType: %d",
                    request.type, device.address, failType);
        }
    }

    private void notifyCharacteristicRead(GenericRequest request, byte[] value) {
        MethodInfo info = MethodInfoGenerator.onCharacteristicRead(request, value);
        handleCallbacks(request.callback, info);
        logD(Logger.TYPE_CHARACTERISTIC_READ, "characteristic read! [UUID: %s, addr: %s, value: %s]",
                substringUuid(request.characteristic), device.address, toHex(value));
    }

    private void notifyCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        MethodInfo info = MethodInfoGenerator.onCharacteristicChanged(device, characteristic.getService().getUuid(),
                characteristic.getUuid(), characteristic.getValue());
        observable.notifyObservers(info);
        if (observer != null) {
            posterDispatcher.post(observer, info);
        }
        logD(Logger.TYPE_CHARACTERISTIC_CHANGED, "characteristic change! [UUID: %s, addr: %s, value: %s]",
                substringUuid(characteristic.getUuid()), device.address, toHex(characteristic.getValue()));
    }

    private void notifyRssiRead(GenericRequest request, int rssi) {
        MethodInfo info = MethodInfoGenerator.onRssiRead(request, rssi);
        handleCallbacks(request.callback, info);
        logD(Logger.TYPE_READ_REMOTE_RSSI, "rssi read! [addr: %s, rssi: %d]", device.address, rssi);
    }

    private void notifyMtuChanged(GenericRequest request, int mtu) {
        MethodInfo info = MethodInfoGenerator.onMtuChanged(request, mtu);
        handleCallbacks(request.callback, info);
        logD(Logger.TYPE_MTU_CHANGED, "mtu change! [addr: %s, mtu: %d]", device.address, mtu);
    }

    private void notifyDescriptorRead(GenericRequest request, byte[] value) {
        MethodInfo info = MethodInfoGenerator.onDescriptorRead(request, value);
        handleCallbacks(request.callback, info);
        logD(Logger.TYPE_DESCRIPTOR_READ, "descriptor read! [UUID: %s, addr: %s, value: %s]",
                substringUuid(request.characteristic), device.address, toHex(value));
    }

    private void notifyNotificationOrIndicationChanged(GenericRequest request, boolean isEnabled) {
        MethodInfo info;
        if (request.type == RequestType.SET_NOTIFICATION) {
            info = MethodInfoGenerator.onNotificationChanged(request, isEnabled);
            logD(Logger.TYPE_NOTIFICATION_CHANGED, "%s [UUID: %s, addr: %s]", isEnabled ? "notification enabled!" :
                    "notification disabled!", substringUuid(request.characteristic), device.address);
        } else {
            info = MethodInfoGenerator.onIndicationChanged(request, isEnabled);
            logD(Logger.TYPE_INDICATION_CHANGED, "%s [UUID: %s, addr: %s]", isEnabled ? "indication enabled!" :
                    "indication disabled!", substringUuid(request.characteristic), device.address);
        }
        handleCallbacks(request.callback, info);
    }

    private void notifyCharacteristicWrite(GenericRequest request, byte[] value) {
        MethodInfo info = MethodInfoGenerator.onCharacteristicWrite(request, value);
        handleCallbacks(request.callback, info);
    }

    private void notifyPhyChange(GenericRequest request, int txPhy, int rxPhy) {
        MethodInfo info = MethodInfoGenerator.onPhyChange(request, txPhy, rxPhy);
        handleCallbacks(request.callback, info);
        String event = request.type == RequestType.READ_PHY ? "phy read!" : "phy update!";
        logD(Logger.TYPE_PHY_CHANGE, "%s [addr: %s, tvPhy: %s, rxPhy: %s]", event, device.address, txPhy, rxPhy);
    }

    @Override
    public int getMtu() {
        return mtu;
    }

    @NonNull
    @Override
    public Device getDevice() {
        return device;
    }

    @Override
    public void reconnect() {
        if (!isReleased) {
            isActiveDisconnect = false;
            tryReconnectCount = 0;
            reconnectImmediatelyCount = 0;
            Message.obtain(connHandler, MSG_DISCONNECT, MSG_ARG_RECONNECT, 0).sendToTarget();
        }
    }

    @Override
    public void disconnect() {
        if (!isReleased) {
            isActiveDisconnect = true;
            Message.obtain(connHandler, MSG_DISCONNECT, MSG_ARG_NONE, 0).sendToTarget();
        }
    }

    //清理内部缓存并强制刷新蓝牙设备的服务
    @SuppressWarnings("all")
    private boolean doRefresh() {
        try {
            Method localMethod = bluetoothGatt.getClass().getMethod("refresh");
            return (boolean) localMethod.invoke(bluetoothGatt);
        } catch (Throwable ignore) {
        }
        return false;
    }

    @Override
    public void refresh() {
        connHandler.sendEmptyMessage(MSG_REFRESH);
    }

    private void release(boolean noEvent) {
        if (!isReleased) {
            isReleased = true;
            configuration.setAutoReconnect(false); //停止自动重连
            connHandler.removeCallbacksAndMessages(null);
            easyBle.removeScanListener(this);
            clearRequestQueueAndNotify();
            if (bluetoothGatt != null) {
                closeGatt(bluetoothGatt);
                bluetoothGatt = null;
            }
            device.connectionState = ConnectionState.RELEASED;
            logD(Logger.TYPE_CONNECTION_STATE, "connection released! [name: %s, addr: %s]", device.name, device.address);
            if (!noEvent) {
                sendConnectionCallback();
            }
            easyBle.releaseConnection(device);//从集合中删除
        }
    }

    @Override
    public void release() {
        release(false);
    }

    @Override
    public void releaseNoEvent() {
        release(true);
    }

    @NonNull
    @Override
    public ConnectionState getConnectionState() {
        return device.connectionState;
    }

    @Override
    public boolean isAutoReconnectEnabled() {
        return configuration.isAutoReconnect;
    }

    @Nullable
    @Override
    public BluetoothGatt getGatt() {
        return bluetoothGatt;
    }

    @Override
    public void clearRequestQueue() {
        synchronized (this) {
            requestQueue.clear();
            currentRequest = null;
        }
    }

    @Override
    public void clearRequestQueueByType(@Nullable RequestType type) {
        synchronized (this) {
            Iterator<GenericRequest> it = requestQueue.iterator();
            while (it.hasNext()) {
                GenericRequest request = it.next();
                if (request.type == type) {
                    it.remove();
                }
            }
            if (currentRequest != null && currentRequest.type == type) {
                currentRequest = null;
            }
        }
    }

    /**
     * 清空请求队列并触发通知事件
     */
    private void clearRequestQueueAndNotify() {
        synchronized (this) {
            for (GenericRequest request : requestQueue) {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, false);
            }
            if (currentRequest != null) {
                handleFailedCallback(currentRequest, REQUEST_FAIL_TYPE_CONNECTION_DISCONNECTED, false);
            }
        }
        clearRequestQueue();
    }

    @NonNull
    @Override
    public ConnectionConfiguration getConnectionConfiguration() {
        return configuration;
    }

    @Nullable
    @Override
    public BluetoothGattService getService(UUID service) {
        if (service != null && bluetoothGatt != null) {
            return bluetoothGatt.getService(service);
        }
        return null;
    }

    @Nullable
    @Override
    public BluetoothGattCharacteristic getCharacteristic(UUID service, UUID characteristic) {
        if (service != null && characteristic != null && bluetoothGatt != null) {
            BluetoothGattService gattService = bluetoothGatt.getService(service);
            if (gattService != null) {
                return gattService.getCharacteristic(characteristic);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public BluetoothGattDescriptor getDescriptor(UUID service, UUID characteristic, UUID descriptor) {
        if (service != null && characteristic != null && descriptor != null && bluetoothGatt != null) {
            BluetoothGattService gattService = bluetoothGatt.getService(service);
            if (gattService != null) {
                BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristic);
                if (gattCharacteristic != null) {
                    return gattCharacteristic.getDescriptor(descriptor);
                }
            }
        }
        return null;
    }

    //检查uuid是否存在，存在则将请求加入队列，不存在则失败回调或通知观察者
    private void checkUuidExistsAndEnqueue(GenericRequest request, int uuidNum) {
        boolean exists = false;
        if (uuidNum > 2) {
            exists = checkDescriptorExists(request, request.service, request.characteristic, request.descriptor);
        } else if (uuidNum > 1) {
            exists = checkCharacteristicExists(request, request.service, request.characteristic);
        } else if (uuidNum == 1) {
            exists = checkServiceExists(request, request.service);
        }
        if (exists) {
            enqueue(request);
        }
    }

    //检查服务是否存在
    private boolean checkServiceExists(GenericRequest request, UUID uuid) {
        if (getService(uuid) == null) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_SERVICE_NOT_EXIST, false);
            return false;
        }
        return true;
    }

    //检查特征是否存在
    private boolean checkCharacteristicExists(GenericRequest request, UUID service, UUID characteristic) {
        if (checkServiceExists(request, service)) {
            if (getCharacteristic(service, characteristic) == null) {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_CHARACTERISTIC_NOT_EXIST, false);
                return false;
            }
            return true;
        }
        return false;
    }

    //检查Descriptor是否存在
    private boolean checkDescriptorExists(GenericRequest request, UUID service, UUID characteristic, UUID descriptor) {
        if (checkServiceExists(request, service) && checkCharacteristicExists(request, service, characteristic)) {
            if (getDescriptor(service, characteristic, descriptor) == null) {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_DESCRIPTOR_NOT_EXIST, false);
                return false;
            }
            return true;
        }
        return false;
    }


    @Override
    public void execute(@NonNull Request request) {
        if (request instanceof GenericRequest) {
            GenericRequest req = (GenericRequest) request;
            req.device = device;
            switch (req.type) {
                case SET_NOTIFICATION:
                case SET_INDICATION:
                case READ_CHARACTERISTIC:
                case WRITE_CHARACTERISTIC:
                    if (req.type == RequestType.WRITE_CHARACTERISTIC && req.writeOptions == null) {
                        //从默认配置中取
                        req.writeOptions = configuration.getDefaultWriteOptions(req.service, req.characteristic);
                        if (req.writeOptions == null) {
                            //没有设置默认的，则新建
                            req.writeOptions = new WriteOptions.Builder().build();
                        }
                    }
                    checkUuidExistsAndEnqueue(req, 2);
                    break;
                case READ_DESCRIPTOR:
                    checkUuidExistsAndEnqueue(req, 3);
                    break;
                default:
                    enqueue(req);
                    break;
            }
        }
    }

    @Deprecated
    @Override
    public boolean isNotificationOrIndicationEnabled(@NonNull BluetoothGattCharacteristic characteristic) {
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(clientCharacteristicConfig);
        return descriptor != null && (Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_INDICATION_VALUE));
    }

    @Deprecated
    @Override
    public boolean isNotificationOrIndicationEnabled(UUID service, UUID characteristic) {
        BluetoothGattCharacteristic c = getCharacteristic(service, characteristic);
        if (c != null) {
            return isNotificationOrIndicationEnabled(c);
        }
        return false;
    }

    @Override
    public boolean isIndicationEnabled(@NonNull BluetoothGattCharacteristic characteristic) {
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(clientCharacteristicConfig);
        return descriptor != null && Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
    }

    @Override
    public boolean isIndicationEnabled(UUID service, UUID characteristic) {
        BluetoothGattCharacteristic c = getCharacteristic(service, characteristic);
        if (c != null) {
            return isIndicationEnabled(c);
        }
        return false;
    }

    @Override
    public boolean isNotificationEnabled(@NonNull BluetoothGattCharacteristic characteristic) {
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(clientCharacteristicConfig);
        return descriptor != null && Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    }

    @Override
    public boolean isNotificationEnabled(UUID service, UUID characteristic) {
        BluetoothGattCharacteristic c = getCharacteristic(service, characteristic);
        if (c != null) {
            return isNotificationEnabled(c);
        }
        return false;
    }
}

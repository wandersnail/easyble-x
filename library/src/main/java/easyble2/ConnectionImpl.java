package easyble2;

import android.bluetooth.*;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import easyble2.callback.*;
import easyble2.util.BleUtils;
import easyble2.util.Logger;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private static final int MSG_RELEASE_CONNECTION = 5;
    private static final int MSG_DISCOVER_SERVICES = 6;
    private static final int MSG_ON_CONNECTION_STATE_CHANGE = 7;
    private static final int MSG_ON_SERVICES_DISCOVERED = 8;

    private static final int MSG_ARG_NONE = 0;
    private static final int MSG_ARG_RECONNECT = 1;
    private static final int MSG_ARG_NOTIFY = 2;
    private static final int MSG_ARG_RELEASE = 3;

    private final BluetoothAdapter bluetoothAdapter;
    private final Device device;
    private final ConnectionConfiguration configuration;//连接配置
    private BluetoothGatt bluetoothGatt;
    private final List<Request> requestQueue = new ArrayList<>();//请求队列
    private Request currentRequest;//当前的请求
    private BluetoothGattCharacteristic pendingCharacteristic;
    private ConnectionStateChangeListener stateChangeListener;//连接状态监听器
    private boolean isReleased;//连接是否已释放
    private final Handler connHandler;//用于操作连接的Handler，运行在主线程
    private CharacteristicChangedCallback characChangedCallback;//特征值变化回调
    private long connStartTime; //用于连接超时计时
    private int refreshCount;//刷新（清缓存）计数，在发现服务后清零
    private int tryReconnectCount;//尝试重连计数
    private int lastConnectionState = -1;//上次连接状态
    private int reconnectImmediatelyCount = 0; //不搜索直接重连计数
    private boolean refreshing;//是否正在执行清理缓存
    private boolean isActiveDisconnect;//是否主动断开连接
    private long lastScanStopTime;//上次搜索停止时间
    private final Logger logger;
    private final EventObservable observable;
    private final Poster poster;
    private final BluetoothGattCallback gattCallback = new BleGattCallback();
    private final EasyBLE easyBle;

    ConnectionImpl(EasyBLE easyBle, BluetoothAdapter bluetoothAdapter, Device device, ConnectionConfiguration configuration, int connectDelay, ConnectionStateChangeListener listener) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.device = device;
        //如果没有配置
        if (configuration == null) {
            this.configuration = new ConnectionConfiguration();
        } else {
            this.configuration = configuration;
        }
        this.easyBle = easyBle;
        logger = easyBle.logger;
        observable = easyBle.eventObservable;
        poster = easyBle.poster;
        connHandler = new ConnHandler(this);
        stateChangeListener = listener;
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
        synchronized (ConnectionImpl.this) {
            lastScanStopTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onScanResult(@NonNull Device device) {
        synchronized (ConnectionImpl.this) {
            if (!isReleased && ConnectionImpl.this.device.equals(device) && device.connectionState == STATE_SCANNING) {
                connHandler.sendEmptyMessage(MSG_CONNECT);
            }
        }
    }

    @Override
    public void onScanError(int errorCode, @NonNull String errorMsg) {

    }

    private class BleGattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (!isReleased) {
                Message.obtain(connHandler, MSG_ON_CONNECTION_STATE_CHANGE, status, newState).sendToTarget();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (!isReleased) {
                Message.obtain(connHandler, MSG_ON_SERVICES_DISCOVERED, status, 0).sendToTarget();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (currentRequest != null) {
                if (currentRequest.type == Request.RequestType.READ_CHARACTERISTIC) {
                    Request request = currentRequest;
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        notifyCharacteristicRead(request.callback, request.tag, characteristic);
                    } else {
                        handleGattStatusFailed();
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (currentRequest != null && currentRequest.waitWriteResult && currentRequest.type == Request.RequestType.WRITE_CHARACTERISTIC) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (currentRequest.remainQueue == null || currentRequest.remainQueue.isEmpty()) {
                        Request request = currentRequest;
                        notifyCharacteristicWrite(request.callback, request.tag, characteristic.getService().getUuid(),
                                characteristic.getUuid(), characteristic.getValue());
                        executeNextRequest();
                    } else {
                        connHandler.removeMessages(MSG_REQUEST_TIMEOUT);
                        connHandler.sendMessageDelayed(Message.obtain(connHandler, MSG_REQUEST_TIMEOUT, currentRequest), configuration.requestTimeoutMillis);
                        Request req = currentRequest;
                        int delay = currentRequest.writeDelay;
                        if (delay > 0) {
                            try {
                                Thread.sleep(delay);
                            } catch (InterruptedException ignored) {
                            }
                            if (req != currentRequest) {
                                return;
                            }
                        }
                        req.sendingBytes = req.remainQueue.remove();
                        write(req, characteristic, req.sendingBytes);
                    }
                } else {
                    handleFailedCallback(currentRequest, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, true);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            notifyCharacteristicChanged(characteristic);
            if (characChangedCallback != null) {
                poster.post(characChangedCallback, MethodInfoGenerator.onCharacteristicChanged(device,
                        characteristic.getService().getUuid(), characteristic.getUuid(), characteristic.getValue()));
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (currentRequest != null) {
                if (currentRequest.type == Request.RequestType.READ_RSSI) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Request request = currentRequest;
                        notifyReadRemoteRssi(request.callback, request.tag, rssi);
                    } else {
                        handleGattStatusFailed();
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (currentRequest != null) {
                BluetoothGattCharacteristic charac = descriptor.getCharacteristic();
                switch (currentRequest.type) {
                    case ENABLE_NOTIFICATION:
                    case DISABLE_NOTIFICATION:
                    case ENABLE_INDICATION:
                    case DISABLE_INDICATION:
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            handleGattStatusFailed();
                        } else if (charac.getService().getUuid() == pendingCharacteristic.getService().getUuid() &&
                                charac.getUuid() == pendingCharacteristic.getUuid()) {
                            boolean isEnableNotify = currentRequest.type == Request.RequestType.ENABLE_NOTIFICATION;
                            if (enableNotificationOrIndicationFail(isEnableNotify || currentRequest.type == Request.RequestType.ENABLE_INDICATION,
                                    isEnableNotify, charac)) {
                                handleGattStatusFailed();
                            }
                        }
                        break;
                    case READ_DESCRIPTOR:
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Request request = currentRequest;
                            notifyDescriptorRead(request.callback, request.tag, descriptor);
                        } else {
                            handleGattStatusFailed();
                        }
                        executeNextRequest();
                        break;
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (currentRequest != null) {
                if (currentRequest.type == Request.RequestType.ENABLE_NOTIFICATION || currentRequest.type == Request.RequestType.DISABLE_NOTIFICATION ||
                        currentRequest.type == Request.RequestType.ENABLE_INDICATION || currentRequest.type == Request.RequestType.DISABLE_INDICATION) {
                    BluetoothGattDescriptor localDescriptor = getDescriptor(descriptor.getCharacteristic().getService().getUuid(),
                            descriptor.getCharacteristic().getUuid(), clientCharacteristicConfig);
                    Request request = currentRequest;
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        handleGattStatusFailed();
                        if (localDescriptor != null) {
                            localDescriptor.setValue(currentRequest.value);
                        }
                    } else {
                        boolean isEnabled = currentRequest.type == Request.RequestType.ENABLE_NOTIFICATION ||
                                currentRequest.type == Request.RequestType.ENABLE_INDICATION;
                        if (request.type == Request.RequestType.ENABLE_NOTIFICATION || request.type == Request.RequestType.DISABLE_NOTIFICATION) {
                            notifyNotificationChanged(request.callback, request.tag, descriptor, isEnabled);
                        } else {
                            notifyIndicationChanged(request.callback, request.tag, descriptor, isEnabled);
                        }
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (currentRequest != null) {
                if (currentRequest.type == Request.RequestType.CHANGE_MTU) {
                    Request request = currentRequest;
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        notifyMtuChanged(request.callback, request.tag, mtu);
                    } else {
                        handleGattStatusFailed();
                    }
                    executeNextRequest();
                }
            }
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            handlePhyReadOrUpdate(true, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            handlePhyReadOrUpdate(false, txPhy, rxPhy, status);
        }
    }

    private void doOnConnectionStateChange(int status, int newState) {
        if (bluetoothGatt != null) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    String msg = String.format(Locale.US, "connected! [name: %s, addr: %s]", device.name, device.address);
                    logger.log(Log.DEBUG, Logger.TYPE_CONNECTION_STATE, msg);
                    device.connectionState = STATE_CONNECTED;
                    sendConnectionCallback();
                    // 延时一会再去发现服务
                    connHandler.sendEmptyMessageDelayed(MSG_DISCOVER_SERVICES, configuration.discoverServicesDelayMillis);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    String msg = String.format(Locale.US, "disconnected! [name: %s, addr: %s, autoReconnEnable: %s]",
                            device.name, device.address, configuration.isAutoReconnect);
                    logger.log(Log.DEBUG, Logger.TYPE_CONNECTION_STATE, msg);
                    clearRequestQueueAndNotify();
                    notifyDisconnected();
                }
            } else {
                String msg = String.format(Locale.US, "GATT error! [status: %d, name: %s, addr: %s]", status, device.name, device.address);
                logger.log(Log.ERROR, Logger.TYPE_CONNECTION_STATE, msg);
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
                String msg = String.format(Locale.US, "services discovered! [name: %s, addr: %s, size: %d]", device.name,
                        device.address, services.size());
                logger.log(Log.DEBUG, Logger.TYPE_CONNECTION_STATE, msg);
                if (services.isEmpty()) {
                    doClearTaskAndRefresh();
                } else {
                    refreshCount = 0;
                    tryReconnectCount = 0;
                    reconnectImmediatelyCount = 0;
                    device.connectionState = STATE_SERVICE_DISCOVERED;
                    sendConnectionCallback();
                }
            } else {
                doClearTaskAndRefresh();
                String msg = String.format(Locale.US, "GATT error! [status: %d, name: %s, addr: %s]", status, device.name, device.address);
                logger.log(Log.ERROR, Logger.TYPE_CONNECTION_STATE, msg);
            }
        }
    }

    private void doDiscoverServices() {
        if (bluetoothGatt != null) {
            bluetoothGatt.discoverServices();
            device.connectionState = STATE_SERVICE_DISCOVERING;
            sendConnectionCallback();
        } else {
            notifyDisconnected();
        }
    }

    private void doTimer() {
        if (!isReleased) {
            //只处理不是已发现服务并且不在刷新也不是主动断开连接的
            if (device.connectionState != STATE_SERVICE_DISCOVERED && !refreshing && !isActiveDisconnect) {
                if (device.connectionState != STATE_DISCONNECTED) {
                    //超时
                    if (System.currentTimeMillis() - connStartTime > configuration.connectTimeoutMillis) {
                        connStartTime = System.currentTimeMillis();
                        String msg = String.format(Locale.US, "connect timeout! [name: %s, addr: %s]", device.name, device.address);
                        logger.log(Log.ERROR, Logger.TYPE_CONNECTION_STATE, msg);
                        int type;
                        switch (device.connectionState) {
                            case STATE_SCANNING:
                                type = TIMEOUT_TYPE_CANNOT_DISCOVER_DEVICE;
                                break;
                            case STATE_CONNECTING:
                                type = TIMEOUT_TYPE_CANNOT_CONNECT;
                                break;
                            default:
                                type = TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES;
                                break;
                        }
                        observable.notifyObservers(MethodInfoGenerator.onConnectTimeout(device, type));
                        if (stateChangeListener != null) {
                            stateChangeListener.onConnectTimeout(device, type);
                        }
                        boolean infinite = configuration.tryReconnectMaxTimes == ConnectionConfiguration.TRY_RECONNECT_TIMES_INFINITE;
                        if (configuration.isAutoReconnect && (infinite || tryReconnectCount < configuration.tryReconnectMaxTimes)) {
                            doDisconnect(true, true, false);
                        } else {
                            doDisconnect(false, true, false);
                            if (stateChangeListener != null) {
                                stateChangeListener.onConnectFailed(device, CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION);
                            }
                            observable.notifyObservers(MethodInfoGenerator.onConnectFailed(device, CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION));
                            String message = String.format(Locale.US, "connect failed! [type: maximun reconnection, name: %s, addr: %s]",
                                    device.name, device.address);
                            logger.log(Log.ERROR, Logger.TYPE_CONNECTION_STATE, message);
                        }
                    }
                } else if (configuration.isAutoReconnect) {
                    doDisconnect(true, true, false);
                }
            }
            connHandler.sendEmptyMessageDelayed(MSG_TIMER, 500);
        }
    }

    private Runnable connectRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isReleased) {
                //连接之前必须先停止搜索
                easyBle.stopScan();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    bluetoothGatt = device.getOriginDevice().connectGatt(easyBle.getContext(), false, gattCallback,
                            configuration.transport, configuration.phy);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bluetoothGatt = device.getOriginDevice().connectGatt(easyBle.getContext(), false, gattCallback,
                            configuration.transport);
                } else {
                    bluetoothGatt = device.getOriginDevice().connectGatt(easyBle.getContext(), false, gattCallback);
                }
            }
        }
    };

    private void doConnect() {
        cancelRefreshState();
        device.connectionState = STATE_CONNECTING;
        sendConnectionCallback();
        String msg = String.format(Locale.US, "connecting [name: %s, addr: %s]", device.name, device.address);
        logger.log(Log.DEBUG, Logger.TYPE_CONNECTION_STATE, msg);
        connHandler.postDelayed(connectRunnable, 500);
    }

    /**
     * 处理断开
     *
     * @param reconnect 断开后是否重连
     * @param notify    是否回调和通知观察者
     * @param release   是否是释放
     */
    private void doDisconnect(boolean reconnect, boolean notify, boolean release) {
        clearRequestQueueAndNotify();
        connHandler.removeCallbacks(connectRunnable);
        connHandler.removeMessages(MSG_DISCOVER_SERVICES);
        if (bluetoothGatt != null) {
            closeGatt(bluetoothGatt);
            bluetoothGatt = null;
        }
        device.connectionState = STATE_DISCONNECTED;
        if (release) {
            device.connectionState = STATE_RELEASED;
            String msg = String.format(Locale.US, "connection released! [name: %s, addr: %s]", device.name, device.address);
            logger.log(Log.DEBUG, Logger.TYPE_CONNECTION_STATE, msg);
            finalRelease();
        } else if (reconnect) {
            if (reconnectImmediatelyCount < configuration.reconnectImmediatelyMaxTimes) {
                tryReconnectCount++;
                reconnectImmediatelyCount++;
                connStartTime = System.currentTimeMillis();
                doConnect();
            } else if (canScanReconnect()) {
                tryScanReconnect();
            }
        }
        if (notify) {
            sendConnectionCallback();
        }
    }

    private void doClearTaskAndRefresh() {
        clearRequestQueueAndNotify();
        doRefresh(true);
    }

    //处理刷新
    private void doRefresh(boolean isAuto) {
        String msg = String.format(Locale.US, "refresh GATT! [name: %s, addr: %s]", device.name, device.address);
        logger.log(Log.DEBUG, Logger.TYPE_CONNECTION_STATE, msg);
        connStartTime = System.currentTimeMillis();
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
            } catch (Exception ignored) {
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
                connHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cancelRefreshState();
                    }
                }, 2000);
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
            connStartTime = System.currentTimeMillis();
            easyBle.stopScan();
            //搜索设备，搜索到才执行连接
            device.connectionState = STATE_SCANNING;
            String msg = String.format(Locale.US, "scanning for reconnection [name: %s, addr: %s]", device.name, device.address);
            logger.log(Log.DEBUG, Logger.TYPE_CONNECTION_STATE, msg);
            easyBle.startScan();
        }
    }

    private boolean canScanReconnect() {
        long duration = System.currentTimeMillis() - lastScanStopTime;
        List<Pair<Integer, Integer>> pairs = configuration.scanIntervalPairsInAutoReonnection;
        Collections.sort(pairs, new Comparator<Pair<Integer, Integer>>() {
            @Override
            public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
                if (o1 == null || o1.first == null) return 1;
                if (o2 == null || o2.first == null) return -1;
                return o2.first.compareTo(o1.first);
            }
        });
        for (Pair<Integer, Integer> pair : pairs) {
            if (pair.first != null && pair.second != null && tryReconnectCount >= pair.first && duration >= pair.second) {
                return true;
            }
        }
        return false;
    }

    private void closeGatt(BluetoothGatt gatt) {
        try {
            gatt.disconnect();
        } catch (Exception ignored) {
        }
        try {
            gatt.close();
        } catch (Exception ignored) {
        }
    }

    private void notifyDisconnected() {
        device.connectionState = STATE_DISCONNECTED;
        sendConnectionCallback();
    }

    private void sendConnectionCallback() {
        if (lastConnectionState != device.connectionState) {
            lastConnectionState = device.connectionState;
            if (stateChangeListener != null) {
                stateChangeListener.onConnectionStateChanged(device);
            }
            observable.notifyObservers(MethodInfoGenerator.onConnectionStateChanged(device));
        }
    }

    private boolean write(Request request, BluetoothGattCharacteristic characteristic, byte[] value) {
        characteristic.setValue(value);
        Integer writeType = configuration.getWriteType(characteristic.getService().getUuid(), characteristic.getUuid());
        if (writeType != null && (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT || writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED ||
                writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)) {
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

    private void handleWriteFailed(Request request) {
        connHandler.removeMessages(MSG_REQUEST_TIMEOUT);
        request.remainQueue = null;
        handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
    }

    private boolean enableNotificationOrIndicationFail(boolean enable, boolean notification, BluetoothGattCharacteristic characteristic) {
        if (!bluetoothAdapter.isEnabled() || bluetoothGatt == null || !bluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            return true;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(clientCharacteristicConfig);
        if (descriptor == null) {
            return true;
        }
        byte[] oriaValue = descriptor.getValue();
        if (currentRequest != null) {
            if (currentRequest.type == Request.RequestType.DISABLE_NOTIFICATION || currentRequest.type == Request.RequestType.ENABLE_NOTIFICATION ||
                    currentRequest.type == Request.RequestType.DISABLE_INDICATION || currentRequest.type == Request.RequestType.ENABLE_INDICATION) {
                currentRequest.value = oriaValue;
            }
        }
        if (enable) {
            descriptor.setValue(notification ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
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
            descriptor.setValue(oriaValue);
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
                        Request request = (Request) msg.obj;
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
                        connection.doDisconnect(reconnect, true, false);
                        break;
                    case MSG_REFRESH://手动刷新
                        connection.doRefresh(false);
                        break;
                    case MSG_RELEASE_CONNECTION://释放连接
                        connection.configuration.setAutoReconnect(false); //停止自动重连
                        connection.doDisconnect(false, msg.arg1 == MSG_ARG_NOTIFY, msg.arg2 == MSG_ARG_RELEASE);
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
                            } else {
                                if (msg.what == MSG_ON_SERVICES_DISCOVERED) {
                                    connection.doOnServicesDiscovered(msg.arg1);
                                } else {
                                    connection.doOnConnectionStateChange(msg.arg1, msg.arg2);
                                }
                            }
                        }
                        break;
                }
            }
        }
    }

    private void enqueue(Request request) {
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
                        Request req = requestQueue.get(i);
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

    private void executeRequest(Request request) {
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
                        if (!bluetoothGatt.requestMtu((int) BleUtils.bytesToNumber(false, request.value))) {
                            handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
                        }
                        break;
                    case READ_PHY:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            bluetoothGatt.readPhy();
                        }
                        break;
                    case SET_PREFERRED_PHY:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            int txPhy = (int) BleUtils.bytesToNumber(false, Arrays.copyOfRange(request.value, 0, 4));
                            int rxPhy = (int) BleUtils.bytesToNumber(false, Arrays.copyOfRange(request.value, 4, 8));
                            int phyOptions = (int) BleUtils.bytesToNumber(false, Arrays.copyOfRange(request.value, 8, 12));
                            bluetoothGatt.setPreferredPhy(txPhy, rxPhy, phyOptions);
                        }
                        break;
                    default:
                        BluetoothGattService gattService = bluetoothGatt.getService(request.serviceUuid);
                        if (gattService != null) {
                            BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(request.characUuid);
                            if (characteristic != null) {
                                switch (request.type) {
                                    case ENABLE_NOTIFICATION:
                                    case DISABLE_NOTIFICATION:
                                    case ENABLE_INDICATION:
                                    case DISABLE_INDICATION:
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
                                handleFailedCallback(request, REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC, true);
                            }
                        } else {
                            handleFailedCallback(request, REQUEST_FAIL_TYPE_NULL_SERVICE, true);
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

    private void executeWriteCharacteristic(Request request, BluetoothGattCharacteristic characteristic) {
        try {
            request.waitWriteResult = configuration.isWaitWriteResult;
            request.writeDelay = configuration.packageWriteDelayMillis;
            int requestWriteDelayMillis = configuration.requestWriteDelayMillis;
            int reqDelay = requestWriteDelayMillis > 0 ? requestWriteDelayMillis : request.writeDelay;
            if (reqDelay > 0) {
                try {
                    Thread.sleep(reqDelay);
                } catch (InterruptedException ignored) {
                }
                if (request != currentRequest) {
                    return;
                }
            }
            if (request.value.length > configuration.packageSize) {
                List<byte[]> list = BleUtils.splitPackage(request.value, configuration.packageSize);
                if (!request.waitWriteResult) { //without waiting
                    int delay = request.writeDelay;
                    for (int i = 0; i < list.size(); i++) {
                        byte[] bytes = list.get(i);
                        if (i > 0 && delay > 0) {
                            try {
                                Thread.sleep(delay);
                            } catch (InterruptedException ignored) {
                            }
                            if (request != currentRequest) {
                                return;
                            }
                        }
                        if (!write(request, characteristic, bytes)) {
                            return;
                        }
                    }
                } else { //发送第一包，剩下的加入队列
                    request.remainQueue = new ConcurrentLinkedQueue<>();
                    request.remainQueue.addAll(list);
                    request.sendingBytes = request.remainQueue.remove();
                    if (!write(request, characteristic, request.sendingBytes)) {
                        return;
                    }
                }
            } else {
                request.sendingBytes = request.value;
                if (!write(request, characteristic, request.value)) {
                    return;
                }
            }
            if (!request.waitWriteResult) {
                notifyCharacteristicWrite(request.callback, request.tag, characteristic.getService().getUuid(), characteristic.getUuid(), request.value);
                executeNextRequest();
            }
        } catch (Exception e) {
            handleWriteFailed(request);
        }
    }

    private void executeReadDescriptor(Request request, BluetoothGattCharacteristic characteristic) {
        BluetoothGattDescriptor gattDescriptor = characteristic.getDescriptor(request.descriptorUuid);
        if (gattDescriptor != null) {
            if (!bluetoothGatt.readDescriptor(gattDescriptor)) {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
            }
        } else {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_NULL_DESCRIPTOR, true);
        }
    }

    private void executeReadCharacteristic(Request request, BluetoothGattCharacteristic characteristic) {
        if (!bluetoothGatt.readCharacteristic(characteristic)) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
        }
    }

    private void executeIndicationOrNotification(Request request, BluetoothGattCharacteristic characteristic) {
        pendingCharacteristic = characteristic;
        BluetoothGattDescriptor gattDescriptor = pendingCharacteristic.getDescriptor(clientCharacteristicConfig);
        if (gattDescriptor == null || !bluetoothGatt.readDescriptor(gattDescriptor)) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_REQUEST_FAILED, true);
        }
    }

    private void handlePhyReadOrUpdate(boolean read, int txPhy, int rxPhy, int status) {
        if (currentRequest != null) {
            if ((read && currentRequest.type == Request.RequestType.READ_PHY) || ((!read && currentRequest.type == Request.RequestType.SET_PREFERRED_PHY))) {
                Request request = currentRequest;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    notifyPhyReadOrUpdate(request.callback, request.tag, read, txPhy, rxPhy);
                } else {
                    handleGattStatusFailed();
                }
                executeNextRequest();
            }
        }
    }

    private void handleGattStatusFailed() {
        if (currentRequest != null) {
            handleFailedCallback(currentRequest, REQUEST_FAIL_TYPE_GATT_STATUS_FAILED, false);
        }
    }

    private void handleFailedCallback(Request request, int failType, boolean executeNext) {
        notifyRequestFialed(request, failType);
        if (executeNext) {
            executeNextRequest();
        }
    }

    private String toHex(byte[] bytes) {
        return BleUtils.bytesToHex(bytes);
    }

    private String substringUuid(UUID uuid) {
        return uuid.toString().substring(0, 8);
    }

    private void notifyRequestFialed(Request request, int failType) {
        MethodInfo info = MethodInfoGenerator.onRequestFailed(device, request, failType);
        if (request.callback != null) {//回调方式
            poster.post(request.callback, info);
        } else {//观察者模式
            observable.notifyObservers(info);
        }
        String msg = String.format(Locale.US, "request failed! [addr: %s, tag: $tag, failType: %d", device.address, failType);
        logger.log(Log.DEBUG, Logger.TYPE_REQUEST_FIALED, msg);
    }

    private void notifyCharacteristicRead(RequestCallback callback, String tag, BluetoothGattCharacteristic characteristic) {
        MethodInfo info = MethodInfoGenerator.onCharacteristicRead(tag, device, characteristic.getService().getUuid(),
                characteristic.getUuid(), characteristic.getValue());
        if (callback != null) {//回调方式
            poster.post(callback, info);
        } else {//观察者模式
            observable.notifyObservers(info);
        }
        String msg = String.format(Locale.US, "characteristic read! [UUID: %s, addr: %s, value: %s]",
                substringUuid(characteristic.getUuid()), device.address, toHex(characteristic.getValue()));
        logger.log(Log.DEBUG, Logger.TYPE_CHARACTERISTIC_READ, msg);
    }

    private void notifyCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        MethodInfo info = MethodInfoGenerator.onCharacteristicChanged(device, characteristic.getService().getUuid(),
                characteristic.getUuid(), characteristic.getValue());
        observable.notifyObservers(info);
        String msg = String.format(Locale.US, "characteristic change! [UUID: %s, addr: %s, value: %s]",
                substringUuid(characteristic.getUuid()), device.address, toHex(characteristic.getValue()));
        logger.log(Log.INFO, Logger.TYPE_CHARACTERISTIC_CHANGED, msg);
    }

    private void notifyReadRemoteRssi(RequestCallback callback, String tag, int rssi) {
        MethodInfo info = MethodInfoGenerator.onRemoteRssiRead(tag, device, rssi);
        if (callback != null) {//回调方式
            poster.post(callback, info);
        } else {//观察者模式
            observable.notifyObservers(info);
        }
        String msg = String.format(Locale.US, "rssi read! [addr: %s, rssi: %d]", device.address, rssi);
        logger.log(Log.DEBUG, Logger.TYPE_READ_REMOTE_RSSI, msg);
    }

    private void notifyMtuChanged(RequestCallback callback, String tag, int mtu) {
        MethodInfo info = MethodInfoGenerator.onMtuChanged(tag, device, mtu);
        if (callback != null) {//回调方式
            poster.post(callback, info);
        } else {//观察者模式
            observable.notifyObservers(info);
        }
        String msg = String.format(Locale.US, "mtu change! [addr: %s, mtu: %d]", device.address, mtu);
        logger.log(Log.DEBUG, Logger.TYPE_MTU_CHANGED, msg);
    }

    private void notifyDescriptorRead(RequestCallback callback, String tag, BluetoothGattDescriptor descriptor) {
        BluetoothGattCharacteristic charac = descriptor.getCharacteristic();
        MethodInfo info = MethodInfoGenerator.onDescriptorRead(tag, device, charac.getService().getUuid(),
                charac.getUuid(), descriptor.getUuid(), descriptor.getValue());
        if (callback != null) {//回调方式
            poster.post(callback, info);
        } else {//观察者模式
            observable.notifyObservers(info);
        }
        String msg = String.format(Locale.US, "descriptor read! [UUID: %s, addr: %s, value: %s]",
                substringUuid(charac.getUuid()), device.address, toHex(charac.getValue()));
        logger.log(Log.DEBUG, Logger.TYPE_DESCRIPTOR_READ, msg);
    }

    private void notifyNotificationChanged(RequestCallback callback, String tag, BluetoothGattDescriptor descriptor, boolean isEnabled) {
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        MethodInfo info = MethodInfoGenerator.onNotificationChanged(tag, device, characteristic.getService().getUuid(),
                characteristic.getUuid(), descriptor.getUuid(), isEnabled);
        if (callback != null) {//回调方式
            poster.post(callback, info);
        } else {//观察者模式
            observable.notifyObservers(info);
        }
        String msg = String.format(Locale.US, "%s [UUID: %s, addr: %s]", isEnabled ? "notification enabled!" : "notification disabled!",
                substringUuid(characteristic.getUuid()), device.address);
        logger.log(Log.DEBUG, Logger.TYPE_NOTIFICATION_CHANGED, msg);
    }

    private void notifyIndicationChanged(RequestCallback callback, String tag, BluetoothGattDescriptor descriptor, boolean isEnabled) {
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        MethodInfo info = MethodInfoGenerator.onIndicationChanged(tag, device, characteristic.getService().getUuid(),
                characteristic.getUuid(), descriptor.getUuid(), isEnabled);
        if (callback != null) {//回调方式
            poster.post(callback, info);
        } else {//观察者模式
            observable.notifyObservers(info);
        }
        String msg = String.format(Locale.US, "%s [UUID: %s, addr: %s]", isEnabled ? "indication enabled!" : "indication disabled!",
                substringUuid(characteristic.getUuid()), device.address);
        logger.log(Log.DEBUG, Logger.TYPE_INDICATION_CHANGED, msg);
    }

    private void notifyCharacteristicWrite(RequestCallback callback, String tag, UUID serviceUuid, UUID characteristicUuid, byte[] value) {
        MethodInfo info = MethodInfoGenerator.onCharacteristicWrite(tag, device, serviceUuid, characteristicUuid, value);
        if (callback != null) {//回调方式
            poster.post(callback, info);
        } else {
            observable.notifyObservers(info);
        }
        String msg = String.format(Locale.US, "write success! [UUID: %s, addr: %s, value: %s]",
                substringUuid(characteristicUuid), device.address, toHex(value));
        logger.log(Log.DEBUG, Logger.TYPE_CHARACTERISTIC_WRITE, msg);
    }

    private void notifyPhyReadOrUpdate(RequestCallback callback, String tag, boolean read, int txPhy, int rxPhy) {
        if (read) {
            MethodInfo info = MethodInfoGenerator.onPhyRead(tag, device, txPhy, rxPhy);
            if (callback != null) {//回调方式
                poster.post(callback, info);
            } else {//观察者模式
                observable.notifyObservers(info);
            }
            logger.log(Log.DEBUG, Logger.TYPE_PHY_READ, "phy read! [addr: ${device.addr}, tvPhy: $txPhy, rxPhy: $rxPhy]");
        } else {
            MethodInfo info = MethodInfoGenerator.onPhyUpdate(tag, device, txPhy, rxPhy);
            if (callback != null) {//回调方式
                poster.post(callback, info);
            } else {//观察者模式
                observable.notifyObservers(info);
            }
            logger.log(Log.DEBUG, Logger.TYPE_PHY_UPDATE, "phy update! [addr: ${device.addr}, tvPhy: $txPhy, rxPhy: $rxPhy]");
        }
    }

    @Override
    public void setCharacteristicChangedCallback(CharacteristicChangedCallback callback) {
        characChangedCallback = callback;
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
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public void refresh() {
        connHandler.sendEmptyMessage(MSG_REFRESH);
    }

    private void finalRelease() {
        isReleased = true;
        connHandler.removeCallbacksAndMessages(null);
        easyBle.removeScanListener(this);
        clearRequestQueueAndNotify();
    }

    @Override
    public void release() {
        Message.obtain(connHandler, MSG_RELEASE_CONNECTION, MSG_ARG_NOTIFY, MSG_ARG_RELEASE).sendToTarget();
    }

    @Override
    public void releaseNoEvnet() {
        Message.obtain(connHandler, MSG_RELEASE_CONNECTION, MSG_ARG_NONE, MSG_ARG_RELEASE).sendToTarget();
    }

    @Override
    public int getConnctionState() {
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
    public void clearRequestQueueByType(@Nullable Request.RequestType type) {
        synchronized (this) {
            Iterator<Request> it = requestQueue.iterator();
            while (it.hasNext()) {
                Request request = it.next();
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
            for (Request request : requestQueue) {
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
    public BluetoothGattService getService(@NonNull UUID serviceUuid) {
        if (bluetoothGatt != null) {
            return bluetoothGatt.getService(serviceUuid);
        }
        return null;
    }

    @Nullable
    @Override
    public BluetoothGattCharacteristic getCharacteristic(@NonNull UUID serviceUuid, @NonNull UUID characteristicUuid) {
        if (bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(serviceUuid);
            if (service != null) {
                return service.getCharacteristic(characteristicUuid);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public BluetoothGattDescriptor getDescriptor(@NonNull UUID serviceUuid, @NonNull UUID characteristicUuid, @NonNull UUID descriptorUuid) {
        if (bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(serviceUuid);
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
                if (characteristic != null) {
                    return characteristic.getDescriptor(descriptorUuid);
                }
            }
        }
        return null;
    }

    //检查uuid是否存在，存在则将请求加入队列，不存在则失败回调或通知观察者
    private void checkUuidExistsAndEnqueue(Request request, UUID... uuids) {
        boolean exists = false;
        if (uuids.length > 2) {
            exists = checkDescriptoreExists(request, uuids[0], uuids[1], uuids[2]);
        } else if (uuids.length > 1) {
            exists = checkCharacteristicExists(request, uuids[0], uuids[1]);
        } else if (uuids.length == 1) {
            exists = checkServiceExists(request, uuids[0]);
        }
        if (exists) {
            enqueue(request);
        }
    }

    //检查服务是否存在
    private boolean checkServiceExists(Request request, UUID uuid) {
        if (getService(uuid) == null) {
            handleFailedCallback(request, REQUEST_FAIL_TYPE_NULL_SERVICE, false);
            return false;
        }
        return true;
    }

    //检查特征是否存在
    private boolean checkCharacteristicExists(Request request, UUID service, UUID characteristic) {
        if (checkServiceExists(request, service)) {
            if (getCharacteristic(service, characteristic) == null) {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_NULL_CHARACTERISTIC, false);
                return false;
            }
            return true;
        }
        return false;
    }

    //检查Descriptore是否存在
    private boolean checkDescriptoreExists(Request request, UUID service, UUID characteristic, UUID descriptor) {
        if (checkServiceExists(request, service) && checkCharacteristicExists(request, service, characteristic)) {
            if (getDescriptor(service, characteristic, descriptor) == null) {
                handleFailedCallback(request, REQUEST_FAIL_TYPE_NULL_DESCRIPTOR, false);
                return false;
            }
            return true;
        }
        return false;
    }
    
    @Override
    public void changeMtu(@Nullable String tag, int mtu) {
        enqueue(Request.newChangeMtuRequest(tag, mtu, 0, null));
    }

    @Override
    public void changeMtu(@Nullable String tag, int mtu, @NonNull MtuChangedCallback callback) {
        enqueue(Request.newChangeMtuRequest(tag, mtu, 0, callback));
    }

    @Override
    public void changeMtu(@Nullable String tag, int mtu, int priority) {
        enqueue(Request.newChangeMtuRequest(tag, mtu, priority, null));
    }

    @Override
    public void changeMtu(@Nullable String tag, int mtu, int priority, @NonNull MtuChangedCallback callback) {
        enqueue(Request.newChangeMtuRequest(tag, mtu, priority, callback));
    }

    @Override
    public void readRssi(@Nullable String tag) {
        enqueue(Request.newReadRssiRequest(tag, 0, null));
    }

    @Override
    public void readRssi(@Nullable String tag, @NonNull RemoteRssiReadCallback callback) {
        enqueue(Request.newReadRssiRequest(tag, 0, callback));
    }

    @Override
    public void readRssi(@Nullable String tag, int priority) {
        enqueue(Request.newReadRssiRequest(tag, priority, null));
    }

    @Override
    public void readRssi(@Nullable String tag, int priority, @NonNull RemoteRssiReadCallback callback) {
        enqueue(Request.newReadRssiRequest(tag, priority, callback));
    }

    @Override
    public void readPhy(@Nullable String tag) {
        enqueue(Request.newReadPhyRequest(tag, 0, null));
    }

    @Override
    public void readPhy(@Nullable String tag, @NonNull PhyReadCallback callback) {
        enqueue(Request.newReadPhyRequest(tag, 0, callback));
    }

    @Override
    public void readPhy(@Nullable String tag, int priority) {
        enqueue(Request.newReadPhyRequest(tag, priority, null));
    }

    @Override
    public void readPhy(@Nullable String tag, int priority, @NonNull PhyReadCallback callback) {
        enqueue(Request.newReadPhyRequest(tag, priority, callback));
    }

    @Override
    public void setPreferredPhy(@Nullable String tag, int txPhy, int rxPhy, int phyOptions) {
        enqueue(Request.newSetPreferredPhyRequest(tag, txPhy, rxPhy, phyOptions, 0, null));
    }

    @Override
    public void setPreferredPhy(@Nullable String tag, int txPhy, int rxPhy, int phyOptions, @NonNull PhyUpdateCallback callback) {
        enqueue(Request.newSetPreferredPhyRequest(tag, txPhy, rxPhy, phyOptions, 0, callback));
    }

    @Override
    public void setPreferredPhy(@Nullable String tag, int txPhy, int rxPhy, int phyOptions, int priority) {
        enqueue(Request.newSetPreferredPhyRequest(tag, txPhy, rxPhy, phyOptions, priority, null));
    }

    @Override
    public void setPreferredPhy(@Nullable String tag, int txPhy, int rxPhy, int phyOptions, int priority, @NonNull PhyUpdateCallback callback) {
        enqueue(Request.newSetPreferredPhyRequest(tag, txPhy, rxPhy, phyOptions, priority, callback));
    }

    @Override
    public void readCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic) {
        Request request = Request.newReadCharacteristicRequest(tag, service, characteristic, 0, null);
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void readCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, int priority) {
        Request request = Request.newReadCharacteristicRequest(tag, service, characteristic, priority, null);
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void readCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull CharacteristicReadCallback callback) {
        Request request = Request.newReadCharacteristicRequest(tag, service, characteristic, 0, callback);
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void readCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, int priority, @NonNull CharacteristicReadCallback callback) {
        Request request = Request.newReadCharacteristicRequest(tag, service, characteristic, priority, callback);
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void writeCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull byte[] value) {
        Inspector.requireNonNull(value, "value is null");
        Request request = Request.newWriteCharacteristicRequest(tag, service, characteristic, value, 0, null);
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void writeCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull byte[] value, int priority) {
        Inspector.requireNonNull(value, "value is null");
        Request request = Request.newWriteCharacteristicRequest(tag, service, characteristic, value, priority, null);
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void writeCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull byte[] value, @NonNull CharacteristicWriteCallback callback) {
        Inspector.requireNonNull(value, "value is null");
        Request request = Request.newWriteCharacteristicRequest(tag, service, characteristic, value, 0, callback);
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void writeCharacteristic(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull byte[] value, int priority, @NonNull CharacteristicWriteCallback callback) {
        Inspector.requireNonNull(value, "value is null");
        Request request = Request.newWriteCharacteristicRequest(tag, service, characteristic, value, priority, callback);
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void setNotificationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled) {
        Request request;
        if (isEnabled) {
            request = Request.newEnableNotificationRequest(tag, service, characteristic, 0, null);
        } else {
            request = Request.newDisableNotificationRequest(tag, service, characteristic, 0, null);
        }
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void setNotificationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled, int priority) {
        Request request;
        if (isEnabled) {
            request = Request.newEnableNotificationRequest(tag, service, characteristic, priority, null);
        } else {
            request = Request.newDisableNotificationRequest(tag, service, characteristic, priority, null);
        }
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void setNotificationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled, @NonNull NotificationChangedCallback callback) {
        Request request;
        if (isEnabled) {
            request = Request.newEnableNotificationRequest(tag, service, characteristic, 0, callback);
        } else {
            request = Request.newDisableNotificationRequest(tag, service, characteristic, 0, callback);
        }
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void setNotificationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled, int priority, @NonNull NotificationChangedCallback callback) {
        Request request;
        if (isEnabled) {
            request = Request.newEnableNotificationRequest(tag, service, characteristic, priority, callback);
        } else {
            request = Request.newDisableNotificationRequest(tag, service, characteristic, priority, callback);
        }
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void setIndicationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled) {
        Inspector.requireNonNull(service, "service' uuid is null");
        Inspector.requireNonNull(characteristic, "characteristic' uuid is null");
        Request request;
        if (isEnabled) {
            request = Request.newEnableIndicationRequest(tag, service, characteristic, 0, null);
        } else {
            request = Request.newDisableIndicationRequest(tag, service, characteristic, 0, null);
        }
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void setIndicationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled, int priority) {
        Request request;
        if (isEnabled) {
            request = Request.newEnableIndicationRequest(tag, service, characteristic, priority, null);
        } else {
            request = Request.newDisableIndicationRequest(tag, service, characteristic, priority, null);
        }
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void setIndicationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled, @NonNull IndicationChangedCallback callback) {
        Request request;
        if (isEnabled) {
            request = Request.newEnableIndicationRequest(tag, service, characteristic, 0, callback);
        } else {
            request = Request.newDisableIndicationRequest(tag, service, characteristic, 0, callback);
        }
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void setIndicationEnabled(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, boolean isEnabled, int priority, @NonNull IndicationChangedCallback callback) {
        Request request;
        if (isEnabled) {
            request = Request.newEnableIndicationRequest(tag, service, characteristic, priority, callback);
        } else {
            request = Request.newDisableIndicationRequest(tag, service, characteristic, priority, callback);
        }
        checkUuidExistsAndEnqueue(request, service, characteristic);
    }

    @Override
    public void readDescriptor(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull UUID descriptor) {
        Request request = Request.newReadDescriptorRequest(tag, service, characteristic, descriptor, 0, null);
        checkUuidExistsAndEnqueue(request, service, characteristic, descriptor);
    }

    @Override
    public void readDescriptor(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull UUID descriptor, int priority) {
        Request request = Request.newReadDescriptorRequest(tag, service, characteristic, descriptor, priority, null);
        checkUuidExistsAndEnqueue(request, service, characteristic, descriptor);
    }

    @Override
    public void readDescriptor(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull UUID descriptor, @NonNull DescriptorReadCallback callback) {
        Request request = Request.newReadDescriptorRequest(tag, service, characteristic, descriptor, 0, callback);
        checkUuidExistsAndEnqueue(request, service, characteristic, descriptor);
    }

    @Override
    public void readDescriptor(@Nullable String tag, @NonNull UUID service, @NonNull UUID characteristic, @NonNull UUID descriptor, int priority, @NonNull DescriptorReadCallback callback) {
        Request request = Request.newReadDescriptorRequest(tag, service, characteristic, descriptor, priority, callback);
        checkUuidExistsAndEnqueue(request, service, characteristic, descriptor);
    }

    @Override
    public boolean isNotificationOrIndicationEnabled(@NonNull BluetoothGattCharacteristic characteristic) {
        Inspector.requireNonNull(characteristic, "characteristic is null");
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(clientCharacteristicConfig);
        return descriptor != null && (Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                Arrays.equals(descriptor.getValue(), BluetoothGattDescriptor.ENABLE_INDICATION_VALUE));
    }

    @Override
    public boolean isNotificationOrIndicationEnabled(@NonNull UUID service, @NonNull UUID characteristic) {
        BluetoothGattCharacteristic c = getCharacteristic(service, characteristic);
        if (c != null) {
            return isNotificationOrIndicationEnabled(c);
        }
        return false;
    }
}

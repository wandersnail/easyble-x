package cn.wandersnail.ble;

import android.bluetooth.BluetoothDevice;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * date: 2019/8/3 17:06
 * author: zengfansheng
 */
public class ConnectionConfiguration {
    /**
     * 无限重连
     */
    public static final int TRY_RECONNECT_TIMES_INFINITE = -1;
    int discoverServicesDelayMillis = 600;
    int connectTimeoutMillis = 10000;
    int requestTimeoutMillis = 3000;
    int tryReconnectMaxTimes = TRY_RECONNECT_TIMES_INFINITE;
    int reconnectImmediatelyMaxTimes = 3;
    boolean isAutoReconnect = true;
    /**
     * 建立连接时是否使用自动连接方式。connectGatt(context,autoConnect,...)方法第二个参数
     */
    boolean useAutoConnect = false;
    boolean stopScanWhenConnecting = true;
    /**
     * 连接失败后重连时使用自动连接方式。connectGatt(context,autoConnect,...)方法第二个参数
     */
    boolean useAutoConnectAfterConnectionFailure = true;
    @RequiresApi(Build.VERSION_CODES.M)
    int transport = BluetoothDevice.TRANSPORT_AUTO;
    @RequiresApi(Build.VERSION_CODES.O)
    int phy = BluetoothDevice.PHY_LE_1M_MASK;
    @NonNull
    final List<Pair<Integer, Integer>> scanIntervalPairsInAutoReconnection;
    private final Map<String, WriteOptions> defaultWriteOptionsMap = new HashMap<>();
    /**
     * 使用读取信号强度辅助判断连接状态
     */
    boolean useReadRemoteRssiToDetectDisconnection = false;

    public ConnectionConfiguration() {
        scanIntervalPairsInAutoReconnection = new ArrayList<>();
        scanIntervalPairsInAutoReconnection.add(Pair.create(0, 2000));
        scanIntervalPairsInAutoReconnection.add(Pair.create(1, 5000));
        scanIntervalPairsInAutoReconnection.add(Pair.create(3, 10000));
        scanIntervalPairsInAutoReconnection.add(Pair.create(5, 30000));
        scanIntervalPairsInAutoReconnection.add(Pair.create(10, 60000));
    }

    /**
     * 连接成功后延时多久开始执行发现服务
     */
    public ConnectionConfiguration setDiscoverServicesDelayMillis(int discoverServicesDelayMillis) {
        this.discoverServicesDelayMillis = discoverServicesDelayMillis;
        return this;
    }

    /**
     * 连接超时时长
     */
    public ConnectionConfiguration setConnectTimeoutMillis(int connectTimeoutMillis) {
        if (requestTimeoutMillis >= 1000) {
            this.connectTimeoutMillis = connectTimeoutMillis;
        }
        return this;
    }

    /**
     * 请求超时时长
     */
    public ConnectionConfiguration setRequestTimeoutMillis(int requestTimeoutMillis) {
        if (requestTimeoutMillis >= 1000) {
            this.requestTimeoutMillis = requestTimeoutMillis;
        }
        return this;
    }

    /**
     * 最大尝试自动重连次数
     */
    public ConnectionConfiguration setTryReconnectMaxTimes(int tryReconnectMaxTimes) {
        this.tryReconnectMaxTimes = tryReconnectMaxTimes;
        return this;
    }

    /**
     * 不经过搜索，直接使用之间的MAC地址连接的次数，重连达到此次数后，恢复搜索到设备再进行连接
     */
    public ConnectionConfiguration setReconnectImmediatelyMaxTimes(int reconnectImmediatelyMaxTimes) {
        this.reconnectImmediatelyMaxTimes = reconnectImmediatelyMaxTimes;
        return this;
    }

    /**
     * 是否自动重连
     */
    public ConnectionConfiguration setAutoReconnect(boolean autoReconnect) {
        isAutoReconnect = autoReconnect;
        return this;
    }

    /**
     * 建立连接时是否使用自动连接方式
     */
    public ConnectionConfiguration setUseAutoConnect(boolean autoConnect) {
        useAutoConnect = autoConnect;
        return this;
    }

    /**
     * 连接失败后重连时使用自动连接方式
     */
    public ConnectionConfiguration setUseAutoConnectAfterConnectionFailure(boolean useAutoConnectAfterConnectionFailure) {
        this.useAutoConnectAfterConnectionFailure = useAutoConnectAfterConnectionFailure;
        return this;
    }

    /**
     * 双模蓝牙的传输模式
     *
     * @param transport {@link BluetoothDevice#TRANSPORT_AUTO}其中之一
     */
    @RequiresApi(Build.VERSION_CODES.M)
    public ConnectionConfiguration setTransport(int transport) {
        this.transport = transport;
        return this;
    }

    /**
     * 物理层的模式
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public ConnectionConfiguration setPhy(int phy) {
        this.phy = phy;
        return this;
    }

    /**
     * 自动重连时，搜索次数与间隔的对应关系，first：已尝试次数，second：间隔，单位为毫秒。如搜索了1次，间隔2秒，搜索了5次，间隔30秒等
     */
    public ConnectionConfiguration setScanIntervalPairsInAutoReconnection(@NonNull List<Pair<Integer, Integer>> parameters) {
        scanIntervalPairsInAutoReconnection.clear();
        scanIntervalPairsInAutoReconnection.addAll(parameters);
        return this;
    }

    /**
     * 设置默认的写特征配置
     *
     * @param service        特征所在的服务UUID
     * @param characteristic 特征的UUID
     * @param options        配置
     */
    public ConnectionConfiguration setDefaultWriteOptions(@NonNull UUID service, @NonNull UUID characteristic, @NonNull WriteOptions options) {
        defaultWriteOptionsMap.put(service + ":" + characteristic, options);
        return this;
    }
    
    @Nullable
    WriteOptions getDefaultWriteOptions(@NonNull UUID service, @NonNull UUID characteristic) {
        return defaultWriteOptionsMap.get(service + ":" + characteristic);
    }

    /**
     * 连接时是否停止搜索。有些蓝牙模块连接之前必须先停止搜索
     *
     * @param stopScanWhenConnecting true：开始连接时，如果正在搜索则自动停止，false：不停止
     */
    public ConnectionConfiguration stopScanWhenConnecting(boolean stopScanWhenConnecting) {
        this.stopScanWhenConnecting = stopScanWhenConnecting;
        return this;
    }

    /**
     * 使用读取信号强度辅助判断连接状态
     *
     * @param useReadRemoteRssiToDetectDisconnection true：使用，false：不使用
     */
    public ConnectionConfiguration setUseReadRemoteRssiToDetectDisconnection(boolean useReadRemoteRssiToDetectDisconnection) {
        this.useReadRemoteRssiToDetectDisconnection = useReadRemoteRssiToDetectDisconnection;
        return this;
    }
}

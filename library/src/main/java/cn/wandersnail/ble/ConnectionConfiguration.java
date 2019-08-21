package cn.wandersnail.ble;

import android.bluetooth.BluetoothDevice;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * date: 2019/8/3 17:06
 * author: zengfansheng
 */
public class ConnectionConfiguration {    
    /**
     * 无限重连
     */
    public static final int TRY_RECONNECT_TIMES_INFINITE = -1;
    int discoverServicesDelayMillis = 500;
    int connectTimeoutMillis = 10000;
    int requestTimeoutMillis = 3000;
    int tryReconnectMaxTimes = TRY_RECONNECT_TIMES_INFINITE;
    int reconnectImmediatelyMaxTimes = 3;
    boolean isAutoReconnect = true;
    @RequiresApi(Build.VERSION_CODES.M)
    int transport = BluetoothDevice.TRANSPORT_LE;
    @RequiresApi(Build.VERSION_CODES.O)
    int phy = BluetoothDevice.PHY_LE_1M_MASK;
    @NonNull
    final List<Pair<Integer, Integer>> scanIntervalPairsInAutoReonnection;
    
    public ConnectionConfiguration() {
        scanIntervalPairsInAutoReonnection = new ArrayList<>();
        scanIntervalPairsInAutoReonnection.add(Pair.create(0, 2000));
        scanIntervalPairsInAutoReonnection.add(Pair.create(1, 5000));
        scanIntervalPairsInAutoReonnection.add(Pair.create(3, 10000));
        scanIntervalPairsInAutoReonnection.add(Pair.create(5, 30000));
        scanIntervalPairsInAutoReonnection.add(Pair.create(10, 60000));
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
    public ConnectionConfiguration setScanIntervalPairsInAutoReonnection(@NonNull List<Pair<Integer, Integer>> parameters) {
        Inspector.requireNonNull(parameters, "parameters is null");
        scanIntervalPairsInAutoReonnection.clear();
        scanIntervalPairsInAutoReonnection.addAll(parameters);
        return this;
    }
}

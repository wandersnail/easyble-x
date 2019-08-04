package easyble2;

import android.bluetooth.BluetoothDevice;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Pair;

import java.util.*;

/**
 * date: 2019/8/3 17:06
 * author: zengfansheng
 */
public class ConnectionConfiguration {    
    /**
     * 无限重连
     */
    public static final int TRY_RECONNECT_TIMES_INFINITE = -1;

    /**
     * 连接成功后延时多久开始执行发现服务
     */
    public int discoverServicesDelayMillis = 500;
    /**
     * 连接超时时长
     */
    public int connectTimeoutMillis = 10000;

    /**
     * 请求超时时长
     */
    public int requestTimeoutMillis = 3000;

    /**
     * 最大尝试自动重连次数
     */
    public int tryReconnectMaxTimes = TRY_RECONNECT_TIMES_INFINITE;

    /**
     * 两次写数据到特征的时间间隔
     */
    public int packageWriteDelayMillis = 0;

    /**
     * 两次写请求的时间间隔，和{@link #packageWriteDelayMillis}不同的是，一次写请求可能会分包发送。
     * 一个是请求与请求的间隔，一个是包与包的间隔
     */
    public int requestWriteDelayMillis = -1;

    /**
     * 一次向特征写入的字节数
     */
    public int packageSize = 20;

    /**
     * 是否等待写入结果回调
     */
    public boolean isWaitWriteResult = true;

    /**
     * 不经过搜索，直接使用之间的MAC地址连接的次数，重连达到此次数后，恢复搜索到设备再进行连接
     */
    public int reconnectImmediatelyMaxTimes = 3;

    private Map<String, Integer> writeTypeMap = new HashMap<>();
    /**
     * 是否自动重连
     */
    public boolean isAutoReconnect = true;

    /**
     * 双模蓝牙的传输模式，{@link BluetoothDevice#TRANSPORT_AUTO}其中之一
     */
    @RequiresApi(Build.VERSION_CODES.M)
    public int transport = BluetoothDevice.TRANSPORT_LE;

    /**
     * 物理层的模式
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public int phy = BluetoothDevice.PHY_LE_1M_MASK;

    /**
     * 自动重连时，搜索次数与间隔的对应关系，first：已尝试次数，second：间隔，单位为毫秒。如搜索了1次，间隔2秒，搜索了5次，间隔30秒等
     */
    @NonNull
    public List<Pair<Integer, Integer>> scanIntervalPairsInAutoReonnection;
    
    public ConnectionConfiguration() {
        scanIntervalPairsInAutoReonnection = new ArrayList<>();
        scanIntervalPairsInAutoReonnection.add(Pair.create(0, 2000));
        scanIntervalPairsInAutoReonnection.add(Pair.create(1, 5000));
        scanIntervalPairsInAutoReonnection.add(Pair.create(3, 10000));
        scanIntervalPairsInAutoReonnection.add(Pair.create(5, 30000));
        scanIntervalPairsInAutoReonnection.add(Pair.create(10, 60000));
    }

    public int getDiscoverServicesDelayMillis() {
        return discoverServicesDelayMillis;
    }

    /**
     * 连接成功后延时多久开始执行发现服务
     */
    public ConnectionConfiguration setDiscoverServicesDelayMillis(int discoverServicesDelayMillis) {
        this.discoverServicesDelayMillis = discoverServicesDelayMillis;
        return this;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
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

    public int getRequestTimeoutMillis() {
        return requestTimeoutMillis;
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

    public int getTryReconnectMaxTimes() {
        return tryReconnectMaxTimes;
    }

    /**
     * 最大尝试自动重连次数
     */
    public ConnectionConfiguration setTryReconnectMaxTimes(int tryReconnectMaxTimes) {
        this.tryReconnectMaxTimes = tryReconnectMaxTimes;
        return this;
    }


    public int getPackageWriteDelayMillis() {
        return packageWriteDelayMillis;
    }

    /**
     * 两次写数据到特征的时间间隔
     */
    public ConnectionConfiguration setPackageWriteDelayMillis(int packageWriteDelayMillis) {
        this.packageWriteDelayMillis = packageWriteDelayMillis;
        return this;
    }


    public int getRequestWriteDelayMillis() {
        return requestWriteDelayMillis;
    }

    /**
     * 两次写请求的时间间隔，和{@link #packageWriteDelayMillis}不同的是，一次写请求可能会分包发送。
     * 一个是请求与请求的间隔，一个是包与包的间隔
     */
    public ConnectionConfiguration setRequestWriteDelayMillis(int requestWriteDelayMillis) {
        this.requestWriteDelayMillis = requestWriteDelayMillis;
        return this;
    }

    public int getPackageSize() {
        return packageSize;
    }

    /**
     * 一次向特征写入的字节数
     */
    public ConnectionConfiguration setPackageSize(int packageSize) {
        if (packageSize > 0) {
            this.packageSize = packageSize;
        }
        return this;
    }

    public boolean isWaitWriteResult() {
        return isWaitWriteResult;
    }

    /**
     * 是否等待写入结果回调
     */
    public ConnectionConfiguration setWaitWriteResult(boolean waitWriteResult) {
        isWaitWriteResult = waitWriteResult;
        return this;
    }

    public int getReconnectImmediatelyMaxTimes() {
        return reconnectImmediatelyMaxTimes;
    }

    /**
     * 不经过搜索，直接使用之间的MAC地址连接的次数，重连达到此次数后，恢复搜索到设备再进行连接
     */
    public ConnectionConfiguration setReconnectImmediatelyMaxTimes(int reconnectImmediatelyMaxTimes) {
        this.reconnectImmediatelyMaxTimes = reconnectImmediatelyMaxTimes;
        return this;
    }

    /**
     * 获取特征的写入模式
     *
     * @param service        服务的UUID
     * @param characteristic 特征的UUID
     * @return 如果没有设置时，返回null
     */
    @Nullable
    public Integer getWriteType(@NonNull UUID service, @NonNull UUID characteristic) {
        return writeTypeMap.get(String.format(Locale.US, "%s:%s", service.toString(), characteristic.toString()));
    }

    /**
     * 设置特征的写入模式
     *
     * @param service        服务的UUID
     * @param characteristic 特征的UUID
     * @param writeType      写入模式
     */
    public ConnectionConfiguration setWriteType(@NonNull UUID service, @NonNull UUID characteristic, int writeType) {
        writeTypeMap.put(String.format(Locale.US, "%s:%s", service.toString(), characteristic.toString()), writeType);
        return this;
    }

    public boolean isAutoReconnect() {
        return isAutoReconnect;
    }

    /**
     * 是否自动重连
     */
    public ConnectionConfiguration setAutoReconnect(boolean autoReconnect) {
        isAutoReconnect = autoReconnect;
        return this;
    }

    @RequiresApi(Build.VERSION_CODES.M)
    public int getTransport() {
        return transport;
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

    @RequiresApi(Build.VERSION_CODES.O)
    public int getPhy() {
        return phy;
    }

    /**
     * 物理层的模式
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public ConnectionConfiguration setPhy(int phy) {
        this.phy = phy;
        return this;
    }

    @NonNull
    public List<Pair<Integer, Integer>> getScanIntervalPairsInAutoReonnection() {
        return scanIntervalPairsInAutoReonnection;
    }

    /**
     * 自动重连时，搜索次数与间隔的对应关系，first：已尝试次数，second：间隔，单位为毫秒。如搜索了1次，间隔2秒，搜索了5次，间隔30秒等
     */
    public ConnectionConfiguration setScanIntervalPairsInAutoReonnection(@NonNull List<Pair<Integer, Integer>> pairs) {
        Objects.requireNonNull(pairs);
        scanIntervalPairsInAutoReonnection = pairs;
        return this;
    }
}

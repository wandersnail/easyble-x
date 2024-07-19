package cn.wandersnail.ble;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.Objects;

import cn.wandersnail.commons.poster.ThreadMode;

/**
 * 搜索配置
 * 
 * date: 2019/8/3 15:31
 * author: zengfansheng
 */
public class ScanConfiguration {
    int scanPeriodMillis = 10000;
    boolean acceptSysConnectedDevice;
    ScanSettings scanSettings;
    boolean onlyAcceptBleDevice;
    int rssiLowLimit = -120;
    List<ScanFilter> filters;
    boolean abortOnLeakPermission = true;
    ScannerType scannerType;
    ThreadMode callbackThreadMode = ThreadMode.MAIN;

    public int getScanPeriodMillis() {
        return scanPeriodMillis;
    }

    public boolean isAcceptSysConnectedDevice() {
        return acceptSysConnectedDevice;
    }

    public ScanSettings getScanSettings() {
        return scanSettings;
    }

    public boolean isOnlyAcceptBleDevice() {
        return onlyAcceptBleDevice;
    }

    public int getRssiLowLimit() {
        return rssiLowLimit;
    }

    public List<ScanFilter> getFilters() {
        return filters;
    }

    public boolean isAbortOnLeakPermission() {
        return abortOnLeakPermission;
    }

    public ThreadMode getCallbackThreadMode() {
        return callbackThreadMode;
    }

    /**
     * 搜索周期
     *
     * @param scanPeriodMillis 搜索一次的毫秒值
     */
    public ScanConfiguration setScanPeriodMillis(int scanPeriodMillis) {
        //至少1秒
        if (scanPeriodMillis >= 1000) {
            this.scanPeriodMillis = scanPeriodMillis;
        }
        return this;
    }

    /**
     * 是否将通过系统蓝牙配对连接的设备添加到搜索结果中（有些手机无法获取到系统已连接的蓝牙设备）
     */
    public ScanConfiguration setAcceptSysConnectedDevice(boolean acceptSysConnectedDevice) {
        this.acceptSysConnectedDevice = acceptSysConnectedDevice;
        return this;
    }

    /**
     * {@link BluetoothLeScanner}的搜索设置
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ScanConfiguration setScanSettings(@NonNull ScanSettings scanSettings) {
        Objects.requireNonNull(scanSettings);
        this.scanSettings = scanSettings;
        return this;
    }

    /**
     * 是否过滤非ble设备
     */
    public ScanConfiguration setOnlyAcceptBleDevice(boolean onlyAcceptBleDevice) {
        this.onlyAcceptBleDevice = onlyAcceptBleDevice;
        return this;
    }

    /**
     * 根据信号强度过滤
     */
    public ScanConfiguration setRssiLowLimit(int rssiLowLimit) {
        this.rssiLowLimit = rssiLowLimit;
        return this;
    }

    /**
     * 设置回调线程模式
     */
    public ScanConfiguration setCallbackThreadMode(@NonNull ThreadMode mode) {
        Objects.requireNonNull(mode);
        this.callbackThreadMode = mode;
        return this;
    }

    /**
     * 搜索过滤器。{@link BluetoothLeScanner#startScan(List, ScanSettings, ScanCallback)}
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ScanConfiguration setFilters(@Nullable List<ScanFilter> filters) {
        this.filters = filters;
        return this;
    }

    /**
     * 缺少权限时是否中断搜索
     *
     * @param abortOnLeakPermission true：缺权限时中断
     */
    public ScanConfiguration setAbortOnLeakPermission(boolean abortOnLeakPermission) {
        this.abortOnLeakPermission = abortOnLeakPermission;
        return this;
    }

    /**
     * 指定蓝牙扫描器，默认为系统Android5.0以上使用{@link ScannerType#LE}，否则使用{@link ScannerType#LEGACY}。
     * 系统小于Android5.0时，指定{@link ScannerType#LE}无效
     */
    public ScanConfiguration setScannerType(@NonNull ScannerType scannerType) {
        Objects.requireNonNull(scannerType);
        this.scannerType = scannerType;
        return this;
    }
}

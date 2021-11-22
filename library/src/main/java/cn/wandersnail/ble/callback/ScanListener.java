package cn.wandersnail.ble.callback;

import android.Manifest;

import androidx.annotation.NonNull;

import cn.wandersnail.ble.Device;

/**
 * 蓝牙搜索监听器
 * <p>
 * date: 2019/8/3 09:17
 * author: zengfansheng
 */
public interface ScanListener {
    /**
     * 缺少定位权限。 {@link Manifest.permission#ACCESS_COARSE_LOCATION} 或者 {@link Manifest.permission#ACCESS_FINE_LOCATION}
     */
    int ERROR_LACK_LOCATION_PERMISSION = 0;
    /**
     * 系统位置服务未开启
     */
    int ERROR_LOCATION_SERVICE_CLOSED = 1;
    /**
     * 搜索错误
     */
    int ERROR_SCAN_FAILED = 2;
    /**
     * 缺少搜索权限。 {@link Manifest.permission#BLUETOOTH_SCAN}
     */
    int ERROR_LACK_SCAN_PERMISSION = 3;
    

    /**
     * 蓝牙搜索开始
     */
    void onScanStart();

    /**
     * 蓝牙搜索停止
     */
    void onScanStop();

    /**
     * 搜索到BLE设备
     *
     * @deprecated 使用 {@link #onScanResult(Device, boolean)}，不要再覆写此方法，因为不再会被回调
     */
    @Deprecated
    default void onScanResult(@NonNull Device device) {
    }

    /**
     * 搜索到BLE设备
     *
     * @param device           搜索到的设备
     * @param isConnectedBySys 是否已被系统蓝牙连接上
     */
    void onScanResult(@NonNull Device device, boolean isConnectedBySys);

    /**
     * 搜索错误
     *
     * @param errorCode {@link #ERROR_LACK_LOCATION_PERMISSION}, {@link #ERROR_LOCATION_SERVICE_CLOSED}
     */
    void onScanError(int errorCode, @NonNull String errorMsg);
}

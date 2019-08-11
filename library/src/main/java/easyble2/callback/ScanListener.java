package easyble2.callback;

import android.Manifest;
import androidx.annotation.NonNull;
import easyble2.Device;

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
     * 蓝牙搜索开始
     */
    void onScanStart();

    /**
     * 蓝牙搜索停止
     */
    void onScanStop();

    /**
     * 搜索到BLE设备
     */
    void onScanResult(@NonNull Device device);

    /**
     * 搜索错误
     *
     * @param errorCode {@link #ERROR_LACK_LOCATION_PERMISSION}, {@link #ERROR_LOCATION_SERVICE_CLOSED}
     */
    void onScanError(int errorCode, @NonNull String errorMsg);
}

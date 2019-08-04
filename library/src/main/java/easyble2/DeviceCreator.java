package easyble2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * {@link Device}实例构建器，搜索到BLE设备时，使用此构建器实例化{@link Device}
 * <p>
 * date: 2019/8/3 00:07
 * author: zengfansheng
 */
public interface DeviceCreator {
    /**
     * 搜索到蓝牙设备后，根据广播数据实例化{@link Device}，并且根据广播过滤设备
     *
     * @param scanResult 搜索的结果数据
     * @return 如果不是需要的设备，返回null，过滤掉，那么它不会触发搜索结果回调，否则返回实例化的{@link Device}，触发搜索结果回调
     */
    @Nullable
    Device create(@NonNull BluetoothDevice device, @Nullable ScanResult scanResult);
}

package easyble2.callback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import easyble2.Device;

/**
 * date: 2019/8/3 17:44
 * author: zengfansheng
 */
public interface RemoteRssiReadCallback extends RequestFailedCallback {
    /**
     * 读取到设备的信号强度
     *
     * @param tag    请求标识
     * @param device 设备
     * @param ssid   信号强度
     */
    void onRemoteRssiRead(@Nullable String tag, @NonNull Device device, int ssid);
}

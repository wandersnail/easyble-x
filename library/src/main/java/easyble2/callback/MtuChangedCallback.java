package easyble2.callback;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import easyble2.Device;

/**
 * date: 2019/8/3 17:42
 * author: zengfansheng
 */
public interface MtuChangedCallback extends RequestFailedCallback {
    /**
     * 最大传输单元变化
     *
     * @param tag    请求标识
     * @param device 设备
     * @param mtu    最大传输单元新的值
     */
    void onMtuChanged(@Nullable String tag, @NonNull Device device, @IntRange(from = 23, to = 517) int mtu);
}

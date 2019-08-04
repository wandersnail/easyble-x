package easyble2.callback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import easyble2.Device;

import java.util.UUID;

/**
 * date: 2019/8/3 17:43
 * author: zengfansheng
 */
public interface NotificationChangedCallback extends RequestFailedCallback {
    /**
     * 通知开关变化
     *
     * @param tag            请求标识
     * @param device         设备
     * @param serviceUuid    服务UUID
     * @param characUuid     特征UUID
     * @param descriptorUuid 描述符UUID
     * @param isEnabled      开启或关闭
     */
    void onNotificationChanged(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid,
                               @NonNull UUID descriptorUuid, boolean isEnabled);
}

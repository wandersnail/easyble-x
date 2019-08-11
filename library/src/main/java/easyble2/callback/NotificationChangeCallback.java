package easyble2.callback;

import androidx.annotation.NonNull;
import easyble2.Request;

/**
 * date: 2019/8/3 17:43
 * author: zengfansheng
 */
public interface NotificationChangeCallback extends RequestFailedCallback {
    /**
     * 通知开关变化 / Indication开关变化
     *
     * @param request   请求
     * @param isEnabled 开启或关闭
     */
    void onNotificationChanged(@NonNull Request request, boolean isEnabled);
}

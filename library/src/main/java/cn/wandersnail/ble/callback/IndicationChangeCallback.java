package cn.wandersnail.ble.callback;

import androidx.annotation.NonNull;

import cn.wandersnail.ble.Request;

/**
 * date: 2019/8/3 17:43
 * author: zengfansheng
 */
public interface IndicationChangeCallback extends RequestFailedCallback {
    /**
     * Indication开关变化
     *
     * @param request   请求
     * @param isEnabled 开启或关闭
     */
    void onIndicationChanged(@NonNull Request request, boolean isEnabled);
}

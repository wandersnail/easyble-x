package cn.wandersnail.ble.callback;

import androidx.annotation.NonNull;

import cn.wandersnail.ble.Request;

/**
 * date: 2019/8/3 18:42
 * author: zengfansheng
 */
public interface ReadCharacteristicCallback extends RequestFailedCallback {
    /**
     * 读取到特征值
     *
     * @param request 请求
     * @param value   读取到的数据
     */
    void onCharacteristicRead(@NonNull Request request, @NonNull byte[] value);
}

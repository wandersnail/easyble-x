package easyble2.callback;

import androidx.annotation.NonNull;
import easyble2.Request;

/**
 * date: 2019/8/3 17:40
 * author: zengfansheng
 */
public interface WriteCharacteristicCallback extends RequestFailedCallback {
    /**
     * 成功写入特征值
     *
     * @param request 请求
     * @param value   写入的数据
     */
    void onCharacteristicWrite(@NonNull Request request, @NonNull byte[] value);
}

package easyble2.callback;

import androidx.annotation.NonNull;
import easyble2.Request;

/**
 * date: 2019/8/3 17:41
 * author: zengfansheng
 */
public interface ReadDescriptorCallback extends RequestFailedCallback {
    /**
     * 读取到描述符值
     *
     * @param request 请求
     * @param value   读取到的数据
     */
    void onDescriptorRead(@NonNull Request request, @NonNull byte[] value);
}

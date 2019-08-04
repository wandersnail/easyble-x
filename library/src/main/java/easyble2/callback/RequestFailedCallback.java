package easyble2.callback;

import androidx.annotation.NonNull;
import easyble2.Connection;
import easyble2.Device;
import easyble2.Request;

/**
 * date: 2019/8/3 17:39
 * author: zengfansheng
 */
public interface RequestFailedCallback extends RequestCallback {
    /**
     * 请求失败
     *
     * @param device   设备
     * @param request  请求实例
     * @param failType 失败类型。{@link Connection#REQUEST_FAIL_TYPE_GATT_IS_NULL}等
     */
    void onRequestFailed(@NonNull Device device, @NonNull Request request, int failType);
}

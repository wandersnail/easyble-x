package easyble2.callback;

import androidx.annotation.NonNull;
import easyble2.Request;

/**
 * date: 2019/8/3 17:42
 * author: zengfansheng
 */
public interface MtuChangeCallback extends RequestFailedCallback {
    /**
     * 最大传输单元变化
     *
     * @param request 请求
     * @param mtu     最大传输单元新的值
     */
    void onMtuChanged(@NonNull Request request, int mtu);
}

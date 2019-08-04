package easyble2.callback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import easyble2.Device;

import java.util.UUID;

/**
 * date: 2019/8/3 17:41
 * author: zengfansheng
 */
public interface DescriptorReadCallback extends RequestFailedCallback {
    /**
     * 读取到描述符值
     *
     * @param tag            请求标识
     * @param device         设备
     * @param serviceUuid    服务UUID
     * @param characUuid     特征UUID
     * @param descriptorUuid 描述符UUID
     * @param value          读取到的数据
     */
    void onDescriptorRead(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid,
                          @NonNull UUID descriptorUuid, @NonNull byte[] value);
}

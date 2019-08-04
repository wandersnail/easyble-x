package easyble2.callback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import easyble2.Device;

import java.util.UUID;

/**
 * date: 2019/8/3 18:42
 * author: zengfansheng
 */
public interface CharacteristicReadCallback extends RequestFailedCallback {
    /**
     * 读取到特征值
     *
     * @param tag         请求标识
     * @param device      设备
     * @param serviceUuid 服务UUID
     * @param characUuid  特征UUID
     * @param value       读取到的数据
     */
    void onCharacteristicRead(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value);
}

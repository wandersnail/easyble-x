package easyble2.callback;

import androidx.annotation.NonNull;
import easyble2.Device;

import java.util.UUID;

/**
 * date: 2019/8/3 17:38
 * author: zengfansheng
 */
public interface CharacteristicChangedCallback extends RequestFailedCallback {
    /**
     * 特征值变化
     *
     * @param device      设备
     * @param serviceUuid 服务UUID
     * @param characUuid  特征UUID
     * @param value       数据
     */
    void onCharacteristicChanged(@NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value);
}

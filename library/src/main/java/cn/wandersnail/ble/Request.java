package cn.wandersnail.ble;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

/**
 * date: 2019/8/11 15:34
 * author: zengfansheng
 */
public interface Request {
    /**
     * 设备
     */
    @NonNull
    Device getDevice();

    /**
     * 请求类型
     */
    @NonNull
    RequestType getType();

    /**
     * 请求标识
     */
    @Nullable
    String getTag();

    /**
     * 服务UUID
     */
    @Nullable
    UUID getService();

    /**
     * 特征UUID
     */
    @Nullable
    UUID getCharacteristic();

    /**
     * 描述符UUID
     */
    @Nullable
    UUID getDescriptor();

    /**
     * 执行请求
     *
     * @param connection 请求执行的连接
     */
    void execute(Connection connection);
}

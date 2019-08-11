package easyble2;

import java.util.Queue;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import easyble2.callback.RequestCallback;

/**
 * date: 2019/8/3 13:44
 * author: zengfansheng
 */
class GenericRequest implements Request, Comparable<GenericRequest> {
    Device device;
    private String tag;
    RequestType type;
    UUID service;
    UUID characteristic;
    UUID descriptor;
    Object value;
    int priority;
    RequestCallback callback;
    WriteOptions writeOptions;
    byte[] descriptorTemp;//临时保存描述符的值
    //---------  分包发送相关  ---------
    Queue<byte[]> remainQueue;
    byte[] sendingBytes;
    //--------------------------------

    GenericRequest(Request.Builder builder) {
        tag = builder.tag;
        type = builder.type;
        service = builder.service;
        characteristic = builder.characteristic;
        descriptor = builder.descriptor;
        priority = builder.priority;
        value = builder.value;
        callback = builder.callback;
        writeOptions = builder.writeOptions;
    }

    @Override
    public int compareTo(@NonNull GenericRequest other) {
        return Integer.compare(other.priority, priority);
    }

    /**
     * 设备
     */
    @NonNull
    public Device getDevice() {
        return device;
    }

    /**
     * 请求类型
     */
    @NonNull
    public RequestType getType() {
        return type;
    }

    /**
     * 请求标识
     */
    @Nullable
    public String getTag() {
        return tag;
    }

    @Nullable
    public UUID getService() {
        return service;
    }

    @Nullable
    public UUID getCharacteristic() {
        return characteristic;
    }

    @Nullable
    public UUID getDescriptor() {
        return descriptor;
    }

    @Override
    public void execute(Connection connection) {
        if (connection != null) {
            connection.execute(this);
        }
    }
}

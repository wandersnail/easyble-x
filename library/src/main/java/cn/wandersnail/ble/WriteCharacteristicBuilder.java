package cn.wandersnail.ble;

import cn.wandersnail.ble.callback.WriteCharacteristicCallback;

/**
 * date: 2019/9/20 18:02
 * author: zengfansheng
 */
public final class WriteCharacteristicBuilder extends RequestBuilder<WriteCharacteristicCallback> {
    WriteCharacteristicBuilder() {
        super(RequestType.WRITE_CHARACTERISTIC);
    }

    @Override
    public WriteCharacteristicBuilder setTag(String tag) {
        super.setTag(tag);
        return this;
    }

    @Override
    public WriteCharacteristicBuilder setPriority(int priority) {
        super.setPriority(priority);
        return this;
    }

    @Override
    public WriteCharacteristicBuilder setCallback(WriteCharacteristicCallback callback) {
        super.setCallback(callback);
        return this;
    }

    /**
     * 设置此次请求的写入设置
     */
    public WriteCharacteristicBuilder setWriteOptions(WriteOptions writeOptions) {
        this.writeOptions = writeOptions;
        return this;
    }
}

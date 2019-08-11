package easyble2;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * 写特征的配置
 * 
 * date: 2019/8/9 18:06
 * author: zengfansheng
 */
public class WriteOptions {
    final int packageWriteDelayMillis;
    final int requestWriteDelayMillis;
    final int packageSize;
    final boolean isWaitWriteResult;
    final int writeType;

    private WriteOptions(Builder builder) {
        packageWriteDelayMillis = builder.packageWriteDelayMillis;
        requestWriteDelayMillis = builder.requestWriteDelayMillis;
        packageSize = builder.packageSize;
        isWaitWriteResult = builder.isWaitWriteResult;
        writeType = builder.writeType;
    }

    /**
     * 两次写数据到特征的时间间隔
     */
    public int getPackageWriteDelayMillis() {
        return packageWriteDelayMillis;
    }

    /**
     * 两次写请求的时间间隔，和{@link #getPackageWriteDelayMillis()}不同的是，一次写请求可能会分包发送。
     * 一个是请求与请求的间隔，一个是包与包的间隔
     */
    public int getRequestWriteDelayMillis() {
        return requestWriteDelayMillis;
    }

    /**
     * 一次向特征写入的字节数
     */
    public int getPackageSize() {
        return packageSize;
    }

    /**
     * 是否等待写入结果回调再写下一包数据
     */
    public boolean isWaitWriteResult() {
        return isWaitWriteResult;
    }

    /**
     * 写入模式
     */
    public int getWriteType() {
        return writeType;
    }

    public static class Builder {
        private int packageWriteDelayMillis = 0;
        private int requestWriteDelayMillis = -1;
        private int packageSize = 20;
        private boolean isWaitWriteResult = true;
        private int writeType = -1;

        /**
         * 两次写数据到特征的时间间隔
         */
        public Builder setPackageWriteDelayMillis(int packageWriteDelayMillis) {
            this.packageWriteDelayMillis = packageWriteDelayMillis;
            return this;
        }

        /**
         * 两次写请求的时间间隔，和{@link #packageWriteDelayMillis}不同的是，一次写请求可能会分包发送。
         * 一个是请求与请求的间隔，一个是包与包的间隔
         */
        public Builder setRequestWriteDelayMillis(int requestWriteDelayMillis) {
            this.requestWriteDelayMillis = requestWriteDelayMillis;
            return this;
        }

        /**
         * 一次向特征写入的字节数
         */
        public Builder setPackageSize(int packageSize) {
            if (packageSize > 0) {
                this.packageSize = packageSize;
            }
            return this;
        }

        /**
         * 是否等待写入结果回调再写下一包数据
         */
        public Builder setWaitWriteResult(boolean waitWriteResult) {
            isWaitWriteResult = waitWriteResult;
            return this;
        }

        /**
         * 设置写入模式
         *
         * @param writeType {@link BluetoothGattCharacteristic#WRITE_TYPE_DEFAULT}
         *                  <br>{@link BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE}
         *                  <br>{@link BluetoothGattCharacteristic#WRITE_TYPE_SIGNED}
         */
        public Builder setWriteType(int writeType) {
            if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT ||
                    writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE ||
                    writeType == BluetoothGattCharacteristic.WRITE_TYPE_SIGNED) {
                this.writeType = writeType;
            }            
            return this;
        }
        
        public WriteOptions build() {
            return new WriteOptions(this);
        }
    }
}

package easyble2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import easyble2.callback.RequestCallback;
import easyble2.util.BleUtils;

import java.util.Arrays;
import java.util.Queue;
import java.util.UUID;

/**
 * date: 2019/8/3 13:44
 * author: zengfansheng
 */
public class Request implements Comparable<Request> {
    String tag;
    RequestType type;
    UUID serviceUuid;
    UUID characUuid;
    UUID descriptorUuid;
    byte[] value;
    int priority;
    RequestCallback callback;
    
    boolean waitWriteResult;
    int writeDelay;
    //---------  分包发送相关  ---------
    Queue<byte[]> remainQueue;
    byte[] sendingBytes;
    //--------------------------------

    private Request(String tag, RequestType type, UUID serviceUuid, UUID characUuid, UUID descriptorUuid, byte[] value, int priority, RequestCallback callback) {
        this.tag = tag;
        this.type = type;
        this.serviceUuid = serviceUuid;
        this.characUuid = characUuid;
        this.descriptorUuid = descriptorUuid;
        this.value = value;
        this.priority = priority;
        this.callback = callback;
    }

    @Override
    public int compareTo(@NonNull Request other) {
        return Integer.compare(other.priority, priority);
    }

    /**
     * 请求类型
     */
    @NonNull
    public RequestType getType() {
        return type;
    }

    /**
     * 请求时设置的标识
     */
    @Nullable
    public String getTag() {
        return tag;
    }

    @Nullable
    public UUID getServiceUuid() {
        return serviceUuid;
    }

    @Nullable
    public UUID getCharacUuid() {
        return characUuid;
    }

    @Nullable
    public UUID getDescriptorUuid() {
        return descriptorUuid;
    }

    /**
     * 请求时携带的数据
     */
    @Nullable
    public byte[] getValue() {
        return Arrays.copyOf(value, value.length);
    }
    
    static Request newChangeMtuRequest(String tag, int mtu, int priority, RequestCallback callback) {
        if (mtu < 23) {
            mtu = 23;
        } else if (mtu > 517) {
            mtu = 517;
        }
        byte[] src = BleUtils.numberToBytes(false, mtu, 4);
        return new Request(tag, RequestType.CHANGE_MTU, null, null, null, src, priority, callback);
    }
    
    static Request newReadCharacteristicRequest(String tag, UUID serviceUuid, UUID characUuid, int priority, RequestCallback callback) {
        return new Request(tag, RequestType.READ_CHARACTERISTIC, serviceUuid, characUuid, null, null, priority, callback);
    }
    
    static Request newEnableNotificationRequest(String tag, UUID serviceUuid, UUID characUuid, int priority, RequestCallback callback) {
        return new Request(tag, RequestType.ENABLE_NOTIFICATION, serviceUuid, characUuid, null, null, priority, callback);
    }

    static Request newDisableNotificationRequest(String tag, UUID serviceUuid, UUID characUuid, int priority, RequestCallback callback) {
        return new Request(tag, RequestType.DISABLE_NOTIFICATION, serviceUuid, characUuid, null, null, priority, callback);
    }

    static Request newEnableIndicationRequest(String tag, UUID serviceUuid, UUID characUuid, int priority, RequestCallback callback) {
        return new Request(tag, RequestType.ENABLE_INDICATION, serviceUuid, characUuid, null, null, priority, callback);
    }

    static Request newDisableIndicationRequest(String tag, UUID serviceUuid, UUID characUuid, int priority, RequestCallback callback) {
        return new Request(tag, RequestType.DISABLE_INDICATION, serviceUuid, characUuid, null, null, priority, callback);
    }

    static Request newReadDescriptorRequest(String tag, UUID serviceUuid, UUID characUuid, UUID descriptorUuid, int priority, RequestCallback callback) {
        return new Request(tag, RequestType.READ_DESCRIPTOR, serviceUuid, characUuid, descriptorUuid, null, priority, callback);
    }

    static Request newWriteCharacteristicRequest(String tag, UUID serviceUuid, UUID characUuid, byte[] value, int priority, RequestCallback callback) {
        return new Request(tag, RequestType.WRITE_CHARACTERISTIC, serviceUuid, characUuid, null, value, priority, callback);
    }

    static Request newReadRssiRequest(String tag, int priority, RequestCallback callback) {
        return new Request(tag, RequestType.READ_RSSI, null, null, null, null, priority, callback);
    }

    static Request newReadPhyRequest(String tag, int priority, RequestCallback callback) {
        return new Request(tag, RequestType.READ_PHY, null, null, null, null, priority, callback);
    }

    static Request newSetPreferredPhyRequest(String tag, int txPhy, int rxPhy, int phyOptions, int priority, RequestCallback callback) {
        byte[] tx = BleUtils.numberToBytes(false, txPhy, 4);
        byte[] rx = BleUtils.numberToBytes(false, rxPhy, 4);
        byte[] options = BleUtils.numberToBytes(false, phyOptions, 4);
        byte[] value = Arrays.copyOf(tx, 12);
        System.arraycopy(rx, 0, value, 4, 4);
        System.arraycopy(options, 0, value, 8, 4);
        return new Request(tag, RequestType.SET_PREFERRED_PHY, null, null, null, value, priority, callback);
    }

    public enum RequestType {
        ENABLE_NOTIFICATION, ENABLE_INDICATION, DISABLE_NOTIFICATION, DISABLE_INDICATION, READ_CHARACTERISTIC, 
        READ_DESCRIPTOR, READ_RSSI, WRITE_CHARACTERISTIC, CHANGE_MTU, READ_PHY, SET_PREFERRED_PHY
    }
}

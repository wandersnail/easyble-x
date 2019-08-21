package cn.wandersnail.ble;


import java.util.UUID;

import cn.wandersnail.commons.poster.MethodInfo;

/**
 * date: 2019/8/4 08:14
 * author: zengfansheng
 */
class MethodInfoGenerator {
    static MethodInfo onBluetoothAdapterStateChanged(int state) {
        return new MethodInfo("onBluetoothAdapterStateChanged", new MethodInfo.Parameter(int.class, state));
    }

    static MethodInfo onConnectionStateChanged(Device device) {
        return new MethodInfo("onConnectionStateChanged", new MethodInfo.Parameter(Device.class, device));
    }

    static MethodInfo onConnectFailed(Device device, int failType) {
        return new MethodInfo("onConnectFailed", new MethodInfo.Parameter(Device.class, device),
                new MethodInfo.Parameter(int.class, failType));
    }

    static MethodInfo onConnectTimeout(Device device, int type) {
        return new MethodInfo("onConnectTimeout", new MethodInfo.Parameter(Device.class, device),
                new MethodInfo.Parameter(int.class, type));
    }

    static MethodInfo onCharacteristicChanged(Device device, UUID service, UUID characteristic, byte[] value) {
        return new MethodInfo("onCharacteristicChanged", new MethodInfo.Parameter(Device.class, device),
                new MethodInfo.Parameter(UUID.class, service), new MethodInfo.Parameter(UUID.class, characteristic), 
                new MethodInfo.Parameter(byte[].class, value));
    }

    static MethodInfo onCharacteristicRead(Request request, byte[] value) {
        return new MethodInfo("onCharacteristicRead", new MethodInfo.Parameter(Request.class, request),
                new MethodInfo.Parameter(byte[].class, value));
    }

    static MethodInfo onCharacteristicWrite(Request request, byte[] value) {
        return new MethodInfo("onCharacteristicWrite", new MethodInfo.Parameter(Request.class, request),
                new MethodInfo.Parameter(byte[].class, value));
    }

    static MethodInfo onRssiRead(Request request, int rssi) {
        return new MethodInfo("onRssiRead", new MethodInfo.Parameter(Request.class, request),
                new MethodInfo.Parameter(int.class, rssi));
    }

    static MethodInfo onDescriptorRead(Request request, byte[] value) {
        return new MethodInfo("onDescriptorRead", new MethodInfo.Parameter(Request.class, request),
                new MethodInfo.Parameter(byte[].class, value));
    }

    static MethodInfo onNotificationChanged(Request request, boolean isEnabled) {
        return new MethodInfo("onNotificationChanged", new MethodInfo.Parameter(Request.class, request),
                new MethodInfo.Parameter(boolean.class, isEnabled));
    }

    static MethodInfo onMtuChanged(Request request, int mtu) {
        return new MethodInfo("onMtuChanged", new MethodInfo.Parameter(Request.class, request),
                new MethodInfo.Parameter(int.class, mtu));
    }

    static MethodInfo onPhyChange(Request request, int txPhy, int rxPhy) {
        return new MethodInfo("onPhyChange", new MethodInfo.Parameter(Request.class, request),
                new MethodInfo.Parameter(int.class, txPhy), new MethodInfo.Parameter(int.class, rxPhy));
    }

    static MethodInfo onRequestFailed(Request request, int failType, Object value) {
        return new MethodInfo("onRequestFailed", new MethodInfo.Parameter(Request.class, request),
                new MethodInfo.Parameter(int.class, failType), new MethodInfo.Parameter(Object.class, value));
    }
}

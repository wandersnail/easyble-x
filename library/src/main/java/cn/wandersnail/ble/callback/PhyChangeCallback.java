package cn.wandersnail.ble.callback;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import cn.wandersnail.ble.Request;

/**
 * date: 2019/8/3 17:43
 * author: zengfansheng
 */
public interface PhyChangeCallback extends RequestFailedCallback {
    /**
     * @param request 请求
     * @param txPhy   物理层发送器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     * @param rxPhy   物理层接收器偏好。{@link BluetoothDevice#PHY_LE_1M_MASK}等
     */
    void onPhyChange(@NonNull Request request, int txPhy, int rxPhy);
}

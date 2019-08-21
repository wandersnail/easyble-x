package cn.wandersnail.ble;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

/**
 * 清空已配对设备时的过滤器
 * <p>
 * date: 2019/8/3 21:11
 * author: zengfansheng
 */
public interface RemoveBondFilter {
    boolean accept(@NonNull BluetoothDevice device);
}

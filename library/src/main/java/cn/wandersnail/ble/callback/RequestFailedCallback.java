package cn.wandersnail.ble.callback;

import android.bluetooth.BluetoothGatt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.wandersnail.ble.Connection;
import cn.wandersnail.ble.Request;

/**
 * date: 2019/8/3 17:39
 * author: zengfansheng
 */
public interface RequestFailedCallback extends RequestCallback {
    /**
     * 请求失败
     *
     * @param request  请求
     * @param failType 失败类型。{@link Connection#REQUEST_FAIL_TYPE_GATT_IS_NULL}等
     * @param value    请求时带的数据，可能为null
     */
    @Deprecated
    default void onRequestFailed(@NonNull Request request, int failType, @Nullable Object value) {}

    /**
     * 请求失败
     *
     * @param request    请求
     * @param failType   失败类型。{@link Connection#REQUEST_FAIL_TYPE_GATT_IS_NULL}等
     * @param gattStatus 状态值。当值为-1时，无效。{@link BluetoothGatt#GATT_READ_NOT_PERMITTED}等
     * @param value      请求时带的数据，可能为null
     */
    void onRequestFailed(@NonNull Request request, int failType, int gattStatus, @Nullable Object value);
}
